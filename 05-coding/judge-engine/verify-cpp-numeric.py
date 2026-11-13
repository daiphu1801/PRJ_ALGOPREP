"""Real-engine regression for the trusted C++ runtime (no learner/DB data).

Usage: python verify-cpp-numeric.py <runtime.txt> <new-evidence.json>
Requires the local go-judge toolchain at 127.0.0.1:5050. Never changes its settings.
"""
import hashlib
import itertools
import json
import math
import pathlib
import random
import re
import sys
import urllib.request

BASE = 'http://127.0.0.1:5050'


def request(path, body=None, method=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data, {'Content-Type': 'application/json'}, method=method)
    with urllib.request.urlopen(req, timeout=20) as response:
        raw = response.read()
        return json.loads(raw) if raw else None


def command(args, stdin='', env=None):
    return {'args': args, 'env': ['PATH=/usr/bin:/bin', 'LC_ALL=C'] + (env or []),
            'files': [{'content': stdin}, {'name': 'stdout', 'max': 131072},
                      {'name': 'stderr', 'max': 65536}],
            'cpuLimit': 10_000_000_000, 'clockLimit': 10_000_000_000,
            'memoryLimit': 1073741824, 'procLimit': 60}


def main():
    runtime_path, evidence_path = map(pathlib.Path, sys.argv[1:])
    if evidence_path.exists():
        raise RuntimeError('Use a new evidence path')
    runtime = runtime_path.read_text(encoding='utf-8')
    source = '#include <bits/stdc++.h>\nusing namespace std;\n' + runtime + r'''
int main(int argc, char** argv) {
    string mode = argc > 1 ? argv[1] : "grammar";
    if (mode == "grammar") {
        string value;
        while (getline(cin, value)) cout << apH_number(value, false) << apH_number(value, true) << '\n';
    } else if (mode == "limit") {
        ApHOutput out; out.add("7"); out.publish();
    } else {
        string value((istreambuf_iterator<char>(cin)), istreambuf_iterator<char>());
        if (mode == "int") cout << apH_scalar<int>(value);
        else if (mode == "long") cout << apH_scalar<long long>(value);
        else cout << setprecision(17) << apH_scalar<double>(value);
    }
}
'''
    compile_cmd = command(['/usr/bin/g++', '-std=c++20', '-O2', '-o', 'main', 'main.cpp'])
    compile_cmd.update(copyIn={'main.cpp': {'content': source}}, copyOutCached=['main'])
    compiled = request('/run', {'cmd': [compile_cmd]})[0]
    ids = compiled.get('fileIds', {})
    evidence = {'runtimeSha256': hashlib.sha256(runtime.encode()).hexdigest(),
                'compileStatus': compiled['status'], 'compileSeconds': 10, 'checks': []}
    try:
        assert compiled['status'] == 'Accepted', compiled['status']

        def run(mode, value='', env=None):
            cmd = command(['/w/main', mode], value, env)
            cmd['copyIn'] = {'main': {'fileId': ids['main']}}
            return request('/run', {'cmd': [cmd]})[0]

        # Exhaustive short strings + seeded long strings; independent regex oracle.
        words = [''.join(x) for n in range(4) for x in itertools.product('01+-.eEx ', repeat=n)]
        rng = random.Random(261113)
        words += [''.join(rng.choices('0123456789+-.eEx \t', k=rng.randrange(1, 35))) for _ in range(3000)]
        words += ['1\x002', '١', '１２', '1.2.3', '.5', '1.', '1.e+2', '-.5e-2', '+', 'e2']
        integer = re.compile(r'[+-]?[0-9]+')
        decimal = re.compile(r'[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?')
        row = run('grammar', '\n'.join(words) + '\n')
        expected = [str(int(bool(integer.fullmatch(s)))) + str(int(bool(decimal.fullmatch(s)))) for s in words]
        assert row['status'] == 'Accepted'
        assert row['files']['stdout'].splitlines() == expected, 'numeric grammar mismatch'
        evidence['checks'].append({'grammarCases': len(words), 'passed': True})

        for mode, valid in [('int', ['0', '+2147483647', '-2147483648', '  +42\r\n', '-0']),
                            ('long', ['9223372036854775807', '-9223372036854775808', '+0007']),
                            ('double', ['.5', '1.', '1.e2', '-.5e-2', '-0.0', '1e-999', '1.7976931348623157e308'])]:
            for value in valid:
                row = run(mode, value)
                assert row['status'] == 'Accepted', (mode, repr(value), row['status'])
                actual = row['files']['stdout']
                assert (math.isclose(float(actual), float(value), rel_tol=1e-15, abs_tol=0)
                        if mode == 'double' else int(actual) == int(value))
                evidence['checks'].append({'mode': mode, 'input': value, 'passed': True})

        for mode, invalid in [('int', ['', '+', '2147483648', '-2147483649', '1.0', '1e2', '0x10', '١', '1\x002']),
                              ('long', ['9223372036854775808', '-9223372036854775809']),
                              ('double', ['.', '1e', '1e+', '1e999', 'nan', 'NaN', 'inf', '0x1p2', '1 2', '--1'])]:
            for value in invalid:
                row = run(mode, value)
                assert row.get('exitStatus') == 90 and '@@APH-HARNESS-INPUT-ERROR@@' in row['files'].get('stderr', ''), (mode, repr(value))
                evidence['checks'].append({'mode': mode, 'input': value, 'rejected': True})

        for value, good in [('65536', True), ('0002', True), ('2', True), ('1', False), ('0', False),
                            ('-1', False), ('+2', False), (' 2', False), ('2 ', False), ('', False),
                            ('9223372036854775808', False), ('18446744073709551616', False), ('2x', False)]:
            row = run('limit', env=['APH_MAX_RESULT_BYTES=' + value])
            assert ((row['status'] == 'Accepted' and row['files']['stdout'] == '\n@@APH-RESULT@@\n7\n') if good
                    else row.get('exitStatus') == 91 and '@@APH-HARNESS-OUTPUT-LIMIT@@' in row['files'].get('stderr', '')), repr(value)
            evidence['checks'].append({'outputLimit': value, 'accepted': good, 'passed': True})
        evidence['passed'] = True
    finally:
        for artifact in ids.values():
            request('/file/' + artifact, method='DELETE')
        evidence_path.write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'passed': True, 'checks': len(evidence['checks']), 'evidence': str(evidence_path)}))


if __name__ == '__main__':
    main()
