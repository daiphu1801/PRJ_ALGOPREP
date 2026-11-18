# Dockerfile cho Judge Engine của AlgoPrep (hỗ trợ Java 21, C++17/20, Python 3)
FROM criyle/go-judge:v1.12.3

# Cài đặt OpenJDK 21 headless, GCC/G++, và Python 3
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless \
    g++ \
    python3 \
    && rm -rf /var/lib/apt/lists/*

# The build script is trusted image content, never supplied by a submission.
COPY build-java.sh /opt/algoprep/build-java.sh
RUN chmod 0555 /opt/algoprep/build-java.sh

# Base sandbox mounts /usr but not /opt. Expose only the trusted image script directory read-only.
RUN python3 -c "from pathlib import Path; p=Path('/opt/mount.yaml'); s=p.read_text(); assert s.count('mount:\n') == 1; p.write_text(s.replace('mount:\n', 'mount:\n  - type: bind\n    source: /opt/algoprep\n    target: /opt/algoprep\n    readonly: true\n', 1))"

# Precompiled header for the C++ wrapper (DEC-2026-1115-cpp-precompiled-header). Compile time of a generated
# wrapper drops from about 1.6 s to 0.75 s because every wrapper only includes <bits/stdc++.h>.
# The flags MUST stay identical to the compile command in ToolchainProfile.java (-std=c++20 -O2); a different flag
# makes g++ ignore the .gch silently. The check below fails the build if the precompiled header is not picked up.
# /usr is bound read-only into the sandbox, so the file next to the header is visible to submissions.
RUN H="$(find /usr/include -path '*/c++/*/bits/stdc++.h' | head -n 1)" \
    && test -n "$H" \
    && g++ -std=c++20 -O2 -x c++-header "$H" -o "$H.gch" \
    && printf '#include <bits/stdc++.h>\nint main() { return 0; }\n' > /tmp/pch-check.cpp \
    && g++ -std=c++20 -O2 -H -fsyntax-only /tmp/pch-check.cpp 2>&1 | grep -q 'stdc++.h.gch' \
    && rm -f /tmp/pch-check.cpp
