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
