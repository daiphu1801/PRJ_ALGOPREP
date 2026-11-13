#include <bits/stdc++.h>
using namespace std;
class Solution {
public:
  int echo(int p0, long long p1, double p2, bool p3, char p4, std::string p5) { return p0; }
};
[[noreturn]] void apH_inputError() {
    std::fputs("@@APH-HARNESS-INPUT-ERROR@@\n", stderr); std::exit(90);
}
[[noreturn]] void apH_outputError() {
    std::fputs("@@APH-HARNESS-OUTPUT-LIMIT@@\n", stderr); std::exit(91);
}
std::string apH_trim(const std::string& value) {
    auto first = value.find_first_not_of(" \t\r\n\f\v");
    return first == std::string::npos ? "" : value.substr(first, value.find_last_not_of(" \t\r\n\f\v") - first + 1);
}
std::vector<std::string> apH_tokens(const std::string& line) {
    std::istringstream input(line); input.imbue(std::locale::classic());
    std::vector<std::string> result;
    for (std::string value; input >> value;) result.push_back(value);
    return result;
}
struct ApHInput {
    std::vector<std::string> lines;
    size_t index = 0;
    ApHInput() {
        std::string text((std::istreambuf_iterator<char>(std::cin)), std::istreambuf_iterator<char>());
        for (size_t i = 0; (i = text.find("\r\n", i)) != std::string::npos;) text.replace(i, 2, "\n");
        std::istringstream input(text);
        for (std::string line; std::getline(input, line);) lines.push_back(line);
    }
    std::string line() {
        if (index == lines.size()) apH_inputError();
        return lines[index++];
    }
    void finish() {
        while (index < lines.size()) if (!apH_trim(lines[index++]).empty()) apH_inputError();
    }
};
// Match the ASCII numeric grammar without instantiating std::regex in every submission.
bool apH_digits(const std::string& value, size_t& index) {
    size_t start = index;
    while (index < value.size() && value[index] >= '0' && value[index] <= '9') ++index;
    return index != start;
}
bool apH_number(const std::string& value, bool decimal) {
    size_t index = 0;
    if (index < value.size() && (value[index] == '+' || value[index] == '-')) ++index;
    bool digits = apH_digits(value, index);
    if (decimal && index < value.size() && value[index] == '.') {
        ++index;
        bool fraction = apH_digits(value, index);
        digits = digits || fraction;
    }
    if (!digits) return false;
    if (decimal && index < value.size() && (value[index] == 'e' || value[index] == 'E')) {
        ++index;
        if (index < value.size() && (value[index] == '+' || value[index] == '-')) ++index;
        if (!apH_digits(value, index)) return false;
    }
    return index == value.size();
}
template<class T> T apH_scalar(const std::string& raw) {
    if constexpr (std::is_same_v<T, std::string>) return raw;
    else {
        std::string value = apH_trim(raw);
        try {
            if constexpr (std::is_same_v<T, bool>) {
                if (value != "true" && value != "false") apH_inputError();
                return value == "true";
            } else if constexpr (std::is_same_v<T, char>) {
                if (raw.size() != 1 || (unsigned char)raw[0] < 33 || (unsigned char)raw[0] > 126) apH_inputError();
                return raw[0];
            } else if constexpr (std::is_integral_v<T>) {
                if (!apH_number(value, false)) apH_inputError();
                long long number = std::stoll(value);
                if (number < std::numeric_limits<T>::min() || number > std::numeric_limits<T>::max()) apH_inputError();
                return static_cast<T>(number);
            } else {
                if (!apH_number(value, true)) apH_inputError();
                char* end;
                double number = std::strtod(value.c_str(), &end);
                if (*end || !std::isfinite(number)) apH_inputError();
                return number;
            }
        } catch (const std::exception&) { apH_inputError(); }
    }
}
int apH_size(const std::string& value) {
    int n = apH_scalar<int>(value);
    if (n < 0) apH_inputError();
    return n;
}
struct ApHOutput {
    std::string text;
    size_t limit;
    ApHOutput() {
        const char* raw = std::getenv("APH_MAX_RESULT_BYTES");
        if (!raw) apH_outputError();
        std::string value(raw);
        size_t end = 0;
        if (!apH_digits(value, end) || end != value.size()) apH_outputError();
        try { limit = std::stoull(raw); } catch (const std::exception&) { apH_outputError(); }
        if (!limit || limit > static_cast<unsigned long long>(LLONG_MAX)) apH_outputError();
    }
    void add(const std::string& value) {
        if (value.size() > limit - text.size()) apH_outputError();
        text += value;
    }
    void publish() {
        add("\n");
        std::string framed = "\n@@APH-RESULT@@\n" + text;
        std::fwrite(framed.data(), 1, framed.size(), stdout);
    }
};
template<class T> struct ApHVector : std::false_type {};
template<class T, class A> struct ApHVector<std::vector<T, A>> : std::true_type {};
template<class T> struct ApHLeaf { using type = T; static constexpr int depth = 0; };
template<class T, class A> struct ApHLeaf<std::vector<T, A>> {
    using type = typename ApHLeaf<T>::type; static constexpr int depth = 1 + ApHLeaf<T>::depth;
};
template<class T> T apH_arrayRows(ApHInput& in, const std::vector<int>& shape, size_t level) {
    T result;
    using Item = typename T::value_type;
    if constexpr (ApHVector<Item>::value) {
        for (int i = 0; i < shape[level]; i++) result.push_back(apH_arrayRows<Item>(in, shape, level + 1));
    } else if constexpr (std::is_same_v<Item, std::string>) {
        for (int i = 0; i < shape[level]; i++) result.push_back(in.line());
    } else {
        auto row = apH_tokens(in.line());
        if (row.size() != static_cast<size_t>(shape[level])) apH_inputError();
        for (auto& value : row) result.push_back(apH_scalar<Item>(value));
    }
    return result;
}
template<class T> T apH_readArray(ApHInput& in, int depth) {
    auto row = apH_tokens(in.line());
    if (row.size() != static_cast<size_t>(depth)) apH_inputError();
    std::vector<int> shape;
    for (auto& value : row) shape.push_back(apH_size(value));
    return apH_arrayRows<T>(in, shape, 0);
}
template<class T> T apH_readList(ApHInput& in) {
    int n = apH_size(in.line());
    T result;
    using Item = typename T::value_type;
    if constexpr (ApHVector<Item>::value) {
        for (int i = 0; i < n; i++) result.push_back(apH_readList<Item>(in));
    } else if constexpr (std::is_same_v<Item, std::string>) {
        for (int i = 0; i < n; i++) result.push_back(in.line());
    } else {
        auto row = apH_tokens(in.line());
        if (row.size() != static_cast<size_t>(n)) apH_inputError();
        for (auto& value : row) result.push_back(apH_scalar<Item>(value));
    }
    return result;
}
template<class Node> Node* apH_readLink(ApHInput& in) {
    using Value = decltype(std::declval<Node>().val);
    auto values = apH_readList<std::vector<Value>>(in);
    Node* head = nullptr; Node* tail = nullptr;
    for (auto value : values) {
        Node* next = new Node(value);
        if (tail) tail->next = next; else head = next;
        tail = next;
    }
    return head;
}
template<class Node> Node* apH_readTree(ApHInput& in) {
    using Value = decltype(std::declval<Node>().val);
    auto row = apH_tokens(in.line());
    if (row.empty() || (row[0] == "#" && row.size() != 1)) apH_inputError();
    if (row[0] == "#") return nullptr;
    Node* root = new Node(apH_scalar<Value>(row[0]));
    std::queue<Node*> queue; queue.push(root);
    size_t index = 1;
    while (index < row.size()) {
        if (queue.empty()) apH_inputError();
        Node* node = queue.front(); queue.pop();
        for (Node** side : {&node->left, &node->right}) {
            if (index == row.size()) break;
            auto token = row[index++];
            if (token != "#") { *side = new Node(apH_scalar<Value>(token)); queue.push(*side); }
        }
    }
    return root;
}
template<class T> std::string apH_text(const T& value) {
    if constexpr (std::is_same_v<T, std::string>) return value;
    else if constexpr (std::is_same_v<T, bool>) return value ? "true" : "false";
    else if constexpr (std::is_same_v<T, char>) {
        if ((unsigned char)value < 33 || (unsigned char)value > 126) throw std::invalid_argument("Invalid result");
        return std::string(1, value);
    } else if constexpr (std::is_floating_point_v<T>) {
        if (std::isnan(value)) return "NaN";
        if (std::isinf(value)) return value < 0 ? "-Infinity" : "Infinity";
        std::ostringstream out; out.imbue(std::locale::classic()); out << std::fixed << std::setprecision(12) << value;
        return out.str() == "-0.000000000000" ? "0.000000000000" : out.str();
    } else return std::to_string(value);
}
void apH_quote(ApHOutput& out, const std::string& value) {
    out.add("\"");
    for (unsigned char c : value) {
        switch (c) {
            case '\\': out.add("\\\\"); break; case '"': out.add("\\\""); break;
            case '\n': out.add("\\n"); break; case '\t': out.add("\\t"); break;
            case '\r': out.add("\\r"); break; case '\b': out.add("\\b"); break; case '\f': out.add("\\f"); break;
            default:
                if (c < 32) { char buffer[7]; std::snprintf(buffer, sizeof(buffer), "\\u%04x", static_cast<unsigned int>(c)); out.add(buffer); }
                else out.add(std::string(1, static_cast<char>(c)));
        }
    }
    out.add("\"");
}
template<class T> void apH_writeList(ApHOutput& out, const T& values) {
    out.add("[");
    bool first = true;
    for (typename T::value_type value : values) {
        if (!first) out.add(","); first = false;
        if constexpr (ApHVector<typename T::value_type>::value) apH_writeList(out, value);
        else if constexpr (std::is_same_v<typename T::value_type, std::string> || std::is_same_v<typename T::value_type, char>) apH_quote(out, apH_text(value));
        else out.add(apH_text(value));
    }
    out.add("]");
}
template<class T> void apH_checkArray(const T& values, std::vector<size_t>& shape, size_t level) {
    if (shape[level] != SIZE_MAX && shape[level] != values.size()) throw std::invalid_argument("Invalid result");
    shape[level] = values.size();
    if constexpr (ApHVector<typename T::value_type>::value) for (const auto& child : values) apH_checkArray(child, shape, level + 1);
}
template<class T> void apH_writeArray(ApHOutput& out, const T& values) {
    std::vector<size_t> shape(ApHLeaf<T>::depth, SIZE_MAX); apH_checkArray(values, shape, 0);
    if constexpr (std::is_same_v<typename ApHLeaf<T>::type, std::string>) apH_writeList(out, values);
    else {
        bool first = true;
        for (typename T::value_type value : values) {
            if (!first) out.add(ApHLeaf<T>::depth == 1 ? " " : ApHLeaf<T>::depth == 2 ? "\n" : "\n\n");
            first = false;
            if constexpr (ApHVector<typename T::value_type>::value) apH_writeArray(out, value);
            else out.add(apH_text(value));
        }
    }
}
template<class Node> void apH_writeLink(ApHOutput& out, Node* head) {
    ApHOutput values;
    size_t count = 0;
    for (Node* node = head; node; node = node->next) {
        if (++count > 1000000) apH_outputError();
        if (count > 1) values.add(" ");
        values.add(apH_text(node->val));
    }
    out.add(std::to_string(count)); out.add("\n"); out.add(values.text);
}
template<class Node> void apH_writeTree(ApHOutput& out, Node* root) {
    if (!root) { out.add("#"); return; }
    std::queue<Node*> queue; queue.push(root);
    size_t count = 0, pending = 0;
    bool first = true;
    while (!queue.empty()) {
        Node* node = queue.front(); queue.pop();
        if (!node) { pending++; continue; }
        if (++count > 1000000) apH_outputError();
        while (pending) { out.add(" #"); pending--; }
        if (!first) out.add(" "); first = false;
        out.add(apH_text(node->val));
        queue.push(node->left); queue.push(node->right);
    }
}
int main() {
  ApHInput apH_in;
  int apH_p0 = apH_scalar<int>(apH_in.line());
  long long apH_p1 = apH_scalar<long long>(apH_in.line());
  double apH_p2 = apH_scalar<double>(apH_in.line());
  bool apH_p3 = apH_scalar<bool>(apH_in.line());
  char apH_p4 = apH_scalar<char>(apH_in.line());
  std::string apH_p5 = apH_scalar<std::string>(apH_in.line());
  apH_in.finish();
  int apH_r = Solution().echo(apH_p0, apH_p1, apH_p2, apH_p3, apH_p4, apH_p5);
  ApHOutput apH_out;
  apH_out.add(apH_text(apH_r));
  apH_out.publish();
}
