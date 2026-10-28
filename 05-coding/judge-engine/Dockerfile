# Dockerfile cho Judge Engine của AlgoPrep (hỗ trợ Java 21, C++17/20, Python 3)
FROM criyle/go-judge:v1.12.3

# Cài đặt OpenJDK 21 headless, GCC/G++, và Python 3
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless \
    g++ \
    python3 \
    && rm -rf /var/lib/apt/lists/*
