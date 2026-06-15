#if __cplusplus < 201700L
#error "C++17 required"
#endif

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <iostream>
#include <limits>
#include <queue>
#include <random>
#include <sstream>
#include <string>
#include <unordered_set>
#include <vector>

// ============================================================
// 数据模型（带底层完美沙盘推演）
// ============================================================

struct Cell {
    int value = 1;
    int  color()       const { return std::abs(value); }
    bool is_bomb()     const { return value < 0; }
    bool is_wildcard() const { return value == 0; }
};

struct Board {
    int N = 0;
    int level = 1;
    std::vector<std::vector<Cell>> grid;

    // 底层引擎变量：未来掉落物预知队列
    std::vector<std::vector<int>> drop_queue;
    std::vector<int> queue_ptr;

    explicit Board(int n = 0) : N(n), grid(n, std::vector<Cell>(n)) {}

    Cell&       at(int r, int c)       { return grid[r][c]; }
    const Cell& at(int r, int c) const { return grid[r][c]; }
    bool in_bounds(int r, int c) const { return r >= 0 && r < N && c >= 0 && c < N; }

    Board preview(const std::vector<std::pair<int,int>>& path) const {
        Board next_b = *this;
        if (path.size() < 2) return next_b;

        std::vector<std::vector<bool>> in_path(N, std::vector<bool>(N, false));
        for (auto [r, c] : path) in_path[r][c] = true;

        std::vector<std::vector<bool>> to_remove = in_path;

        if (level >= 4) {
            for (auto [r, c] : path) {
                if (!at(r, c).is_bomb()) continue;
                for (int dr = -1; dr <= 1; ++dr) {
                    for (int dc = -1; dc <= 1; ++dc) {
                        int nr = r + dr, nc = c + dc;
                        if (in_bounds(nr, nc) && !in_path[nr][nc]) {
                            to_remove[nr][nc] = true;
                        }
                    }
                }
            }
        }

        for (int c = 0; c < N; ++c) {
            std::vector<Cell> remaining;
            for (int r = 0; r < N; ++r) {
                if (!to_remove[r][c]) remaining.push_back(at(r, c));
            }
            int empty = N - static_cast<int>(remaining.size());
            for (int i = 0; i < empty; ++i) {
                int val = next_b.drop_queue[c][next_b.queue_ptr[c]++];
                next_b.at(i, c).value = val;
            }
            for (int i = 0; i < static_cast<int>(remaining.size()); ++i) {
                next_b.at(empty + i, c) = remaining[i];
            }
        }
        return next_b;
    }

    bool is_deadlocked() const {
        for (int r = 0; r < N; ++r) {
            for (int c = 0; c < N; ++c) {
                int ac = at(r, c).color();
                if (c + 1 < N) {
                    int ac2 = at(r, c + 1).color();
                    if (ac == ac2 || ac == 0 || ac2 == 0) return false;
                }
                if (r + 1 < N) {
                    int ac2 = at(r + 1, c).color();
                    if (ac == ac2 || ac == 0 || ac2 == 0) return false;
                }
            }
        }
        return true;
    }
};

// ============================================================
// 中间件：GameController
// ============================================================

class GameController {
    Board _board;
    int   _level = 0;
    int   _step  = 0;
    int   _score = 0;
    bool  _done  = false;
    std::string _pending_line;
    std::vector<std::pair<int,int>> _last_path;

    static int try_parse_level(const std::string& line, int& level, int& seed) {
        int lv, sd, N, steps;
        if (std::sscanf(line.c_str(), "LEVEL %d SEED %d SIZE %d STEPS %d", &lv, &sd, &N, &steps) == 4) {
            level = lv;
            seed = sd;
            return N;
        }
        return 0;
    }

    static bool try_parse_step(const std::string& line, int& step, int& score, bool& valid) {
        char buf[16] = {};
        if (std::sscanf(line.c_str(), "STEP %d SCORE %d %15s", &step, &score, buf) >= 3) {
            valid = (std::string(buf) == "VALID");
            return true;
        }
        return false;
    }

    static int gen_block(std::mt19937& rng, int level) {
        if (level <= 2) {
            return (rng() % 5) + 1;
        }
        if (level == 3) {
            return ((rng() % 100) < 15) ? 0 : (rng() % 5) + 1;
        }
        if (level == 4) {
            int color = (rng() % 5) + 1;
            return ((rng() % 100) < 10) ? -color : color;
        }
        if ((rng() % 100) < 15) return 0;
        int base = (rng() % 5) + 1;
        return ((rng() % 100) < 10) ? -base : base;
    }

    static void init_queues(Board& b, int seed, int N, int level) {
        b.level = level;
        std::mt19937 rng(seed);
        b.drop_queue.assign(N, std::vector<int>(1000));
        b.queue_ptr.assign(N, 0);
        for (int c = 0; c < N; ++c) {
            for (int i = 0; i < 1000; ++i) {
                b.drop_queue[c][i] = gen_block(rng, level);
            }
        }
    }

    bool read_line(std::string& line) {
        if (!_pending_line.empty()) {
            line = std::move(_pending_line);
            _pending_line.clear();
            return true;
        }
        return static_cast<bool>(std::getline(std::cin, line));
    }

    Board read_board(int N) {
        Board board(N);
        for (int row = 0; row < N; ++row) {
            std::string line;
            read_line(line);
            std::istringstream ls(line);
            for (int c = 0; c < N; ++c) ls >> board.at(row, c).value;
        }
        return board;
    }

    void drain_trailing() {
        std::string line;
        while (std::cin.rdbuf()->in_avail() > 0) {
            if (!read_line(line)) break;
            if (line.empty() || line.find("LEVEL_END") != std::string::npos) continue;
            if (line.find("FINAL_SCORE") != std::string::npos) {
                _done = true;
                continue;
            }
            _pending_line = std::move(line);
            break;
        }
    }

public:
    const Board& board() const { return _board; }
    int level() const { return _level; }
    int step()  const { return _step;  }
    int score() const { return _score; }
    bool done() const { return _done;  }

    bool update() {
        std::string first_line;
        while (true) {
            if (!read_line(first_line)) {
                _done = true;
                return false;
            }
            if (!first_line.empty()) break;
        }

        if (first_line.find("LEVEL_END") != std::string::npos ||
            first_line.find("FINAL_SCORE") != std::string::npos) {
            _done = true;
            return false;
        }

        int seed = 0;
        int new_N = try_parse_level(first_line, _level, seed);
        if (new_N > 0) {
            Board new_board = read_board(new_N);
            init_queues(new_board, seed, new_N, _level);
            _board = std::move(new_board);
            _step = 0;
            _score = 0;
            drain_trailing();
            return true;
        }

        int step = 0;
        int score = 0;
        bool valid = false;
        if (try_parse_step(first_line, step, score, valid)) {
            _step = step;
            _score = score;

            Board predicted = (valid && !_last_path.empty()) ? _board.preview(_last_path) : _board;

            Board new_board = read_board(_board.N);
            new_board.level = _level;
            new_board.drop_queue = std::move(predicted.drop_queue);
            new_board.queue_ptr = std::move(predicted.queue_ptr);
            _board = std::move(new_board);
            _last_path.clear();

            drain_trailing();
            if (!_pending_line.empty()) {
                int next_level = 0, next_seed = 0;
                int next_N = try_parse_level(_pending_line, next_level, next_seed);
                if (next_N > 0) {
                    _level = next_level;
                    _pending_line.clear();
                    Board nb = read_board(next_N);
                    init_queues(nb, next_seed, next_N, next_level);
                    _board = std::move(nb);
                    _step = 0;
                    _score = 0;
                    drain_trailing();
                }
            }
            return true;
        }

        _done = true;
        return false;
    }

    void respond(const std::vector<std::pair<int,int>>& path) {
        _last_path = path;
        std::cout << path.size();
        for (auto [r, c] : path) std::cout << ' ' << r << ' ' << c;
        std::cout << '\n';
        std::cout.flush();
    }
};

// ============================================================
// 工具函数
// ============================================================

constexpr int DR[] = {-1, 1, 0, 0};
constexpr int DC[] = { 0, 0,-1, 1};

int path_score(int k) {
    double t = std::sqrt(static_cast<double>(k)) - 1.0;
    return 10 * k + 18 * static_cast<int>(t * t);
}

int path_score(const Board& board, const std::vector<std::pair<int,int>>& path) {
    int s = path_score(static_cast<int>(path.size()));
    if (board.level < 4) return s;

    std::vector<std::vector<bool>> in_path(board.N, std::vector<bool>(board.N, false));
    std::vector<std::vector<bool>> exploded(board.N, std::vector<bool>(board.N, false));
    for (auto [r, c] : path) in_path[r][c] = true;

    for (auto [r, c] : path) {
        if (!board.at(r, c).is_bomb()) continue;
        for (int dr = -1; dr <= 1; ++dr) {
            for (int dc = -1; dc <= 1; ++dc) {
                int nr = r + dr, nc = c + dc;
                if (board.in_bounds(nr, nc) && !in_path[nr][nc] && !exploded[nr][nc]) {
                    exploded[nr][nc] = true;
                    s += 10;
                }
            }
        }
    }
    return s;
}

// ============================================================
// 更高上限的搜索策略
// ============================================================

struct MoveCandidate {
    std::vector<std::pair<int,int>> path;
    int score = 0;
    int length = 0;
    int bomb_count = 0;
};

static bool better_candidate(const MoveCandidate& a, const MoveCandidate& b) {
    if (a.score != b.score) return a.score > b.score;
    if (a.length != b.length) return a.length > b.length;
    return a.bomb_count > b.bomb_count;
}

static uint64_t hash_path_indices(const std::vector<int>& path) {
    uint64_t h = 1469598103934665603ULL;
    for (int x : path) {
        h ^= static_cast<uint64_t>(x + 1);
        h *= 1099511628211ULL;
    }
    h ^= static_cast<uint64_t>(path.size()) << 32;
    return h;
}

static uint64_t hash_path_pairs(const std::vector<std::pair<int,int>>& path, int N) {
    uint64_t h = 1469598103934665603ULL;
    for (auto [r, c] : path) {
        int idx = r * N + c;
        h ^= static_cast<uint64_t>(idx + 1);
        h *= 1099511628211ULL;
    }
    h ^= static_cast<uint64_t>(path.size()) << 32;
    return h;
}

class CandidatePool {
    int _limit = 0;
    std::vector<MoveCandidate> _items;
    std::unordered_set<uint64_t> _seen;

    static bool better_triplet(int score_a, int len_a, int bombs_a,
                               int score_b, int len_b, int bombs_b) {
        if (score_a != score_b) return score_a > score_b;
        if (len_a != len_b) return len_a > len_b;
        return bombs_a > bombs_b;
    }

    void sort_trim() {
        std::sort(_items.begin(), _items.end(), better_candidate);
        if (static_cast<int>(_items.size()) > _limit) {
            _items.resize(_limit);
        }
    }

public:
    explicit CandidatePool(int limit = 0) : _limit(limit) {}

    bool full() const {
        return _limit > 0 && static_cast<int>(_items.size()) >= _limit;
    }

    int worst_score() const {
        if (_items.empty()) return std::numeric_limits<int>::min();
        return _items.back().score;
    }

    bool worth_consider(int score, int length, int bomb_count) const {
        if (_limit <= 0) return false;
        if (static_cast<int>(_items.size()) < _limit) return true;
        const MoveCandidate& worst = _items.back();
        return better_triplet(score, length, bomb_count,
                              worst.score, worst.length, worst.bomb_count);
    }

    void add_from_indices(const std::vector<int>& idx_path, int N, int score, int bomb_count) {
        int length = static_cast<int>(idx_path.size());
        if (!worth_consider(score, length, bomb_count)) return;
        uint64_t h = hash_path_indices(idx_path);
        if (!_seen.insert(h).second) return;

        MoveCandidate cand;
        cand.score = score;
        cand.length = length;
        cand.bomb_count = bomb_count;
        cand.path.reserve(idx_path.size());
        for (int idx : idx_path) {
            cand.path.push_back({idx / N, idx % N});
        }
        _items.push_back(std::move(cand));
        sort_trim();
    }

    void add_candidate(const MoveCandidate& cand, int N) {
        if (!worth_consider(cand.score, cand.length, cand.bomb_count)) return;
        uint64_t h = hash_path_pairs(cand.path, N);
        if (!_seen.insert(h).second) return;
        _items.push_back(cand);
        sort_trim();
    }

    const std::vector<MoveCandidate>& items() const { return _items; }
};

struct ComponentInfo {
    std::vector<int> cells;
    int anchor = 1;
    int bomb_count = 0;
    int wildcard_count = 0;
    bool has_colored = false;
    int optimistic_score = 0;
};

struct CandidateGenConfig {
    int total_budget = 0;
    int component_budget_base = 0;
    int component_budget_per_cell = 0;
    int candidate_limit = 0;
    int per_component_limit = 0;
    int start_limit = 0;
};

struct SearchStage {
    CandidateGenConfig gen;
    int branch_width = 1;
    int deadlock_penalty = 0;
    double future_weight = 0.0;
    double immediate_weight = 1.0;
};

struct ComponentSearchResult {
    std::vector<MoveCandidate> candidates;
    int nodes_used = 0;
};

static bool matches_anchor(const Cell& cell, int anchor) {
    return cell.color() == 0 || cell.color() == anchor;
}

static std::vector<ComponentInfo> collect_components(const Board& board) {
    int N = board.N;
    int total = N * N;
    std::vector<ComponentInfo> comps;
    std::vector<unsigned char> wildcard_only_seen(total, 0);

    for (int anchor = 1; anchor <= 5; ++anchor) {
        std::vector<unsigned char> seen(total, 0);
        for (int idx = 0; idx < total; ++idx) {
            if (seen[idx]) continue;
            int r = idx / N;
            int c = idx % N;
            if (!matches_anchor(board.at(r, c), anchor)) continue;

            std::queue<int> q;
            q.push(idx);
            seen[idx] = 1;

            ComponentInfo comp;
            comp.anchor = anchor;

            while (!q.empty()) {
                int cur = q.front();
                q.pop();

                int cr = cur / N;
                int cc = cur % N;
                const Cell& cell = board.at(cr, cc);

                comp.cells.push_back(cur);
                if (cell.color() != 0) comp.has_colored = true;
                if (cell.is_bomb()) ++comp.bomb_count;
                if (cell.is_wildcard()) ++comp.wildcard_count;

                for (int d = 0; d < 4; ++d) {
                    int nr = cr + DR[d];
                    int nc = cc + DC[d];
                    if (!board.in_bounds(nr, nc)) continue;
                    int ni = nr * N + nc;
                    if (seen[ni]) continue;
                    if (!matches_anchor(board.at(nr, nc), anchor)) continue;
                    seen[ni] = 1;
                    q.push(ni);
                }
            }

            if (static_cast<int>(comp.cells.size()) < 2) continue;
            if (!comp.has_colored) {
                if (wildcard_only_seen[comp.cells.front()]) continue;
                for (int cell_idx : comp.cells) wildcard_only_seen[cell_idx] = 1;
            }

            comp.optimistic_score = path_score(static_cast<int>(comp.cells.size()));
            if (board.level >= 4) {
                int max_bonus_cells = std::min(board.N * board.N - 2, 8 * comp.bomb_count);
                comp.optimistic_score += 10 * max_bonus_cells;
            }
            comps.push_back(std::move(comp));
        }
    }

    std::sort(comps.begin(), comps.end(), [](const ComponentInfo& a, const ComponentInfo& b) {
        if (a.optimistic_score != b.optimistic_score) return a.optimistic_score > b.optimistic_score;
        if (a.cells.size() != b.cells.size()) return a.cells.size() > b.cells.size();
        return a.bomb_count > b.bomb_count;
    });
    return comps;
}

static ComponentSearchResult search_component(const Board& board,
                                              const ComponentInfo& comp,
                                              int node_budget,
                                              int start_limit,
                                              int local_limit) {
    ComponentSearchResult result;
    if (node_budget <= 0 || local_limit <= 0) return result;

    int N = board.N;
    int total = N * N;
    bool bomb_level = (board.level >= 4);

    std::vector<unsigned char> in_comp(total, 0);
    for (int idx : comp.cells) in_comp[idx] = 1;

    std::vector<int> degree(total, 0);
    for (int idx : comp.cells) {
        int r = idx / N;
        int c = idx % N;
        int deg = 0;
        for (int d = 0; d < 4; ++d) {
            int nr = r + DR[d], nc = c + DC[d];
            if (!board.in_bounds(nr, nc)) continue;
            int ni = nr * N + nc;
            if (in_comp[ni]) ++deg;
        }
        degree[idx] = deg;
    }

    std::vector<int> starts = comp.cells;
    std::sort(starts.begin(), starts.end(), [&](int a, int b) {
        const Cell& ca = board.at(a / N, a % N);
        const Cell& cb = board.at(b / N, b % N);
        if (degree[a] != degree[b]) return degree[a] < degree[b];
        if (ca.is_bomb() != cb.is_bomb()) return ca.is_bomb() && !cb.is_bomb();
        if (ca.is_wildcard() != cb.is_wildcard()) return ca.is_wildcard() && !cb.is_wildcard();
        int ea = std::min({a / N, a % N, N - 1 - a / N, N - 1 - a % N});
        int eb = std::min({b / N, b % N, N - 1 - b / N, N - 1 - b % N});
        return ea < eb;
    });
    if (static_cast<int>(starts.size()) > start_limit) starts.resize(start_limit);

    CandidatePool pool(local_limit);
    std::vector<unsigned char> visited(total, 0);
    std::vector<int> cover(total, 0);
    std::vector<int> bfs_mark(total, 0);
    std::vector<int> path;
    path.reserve(comp.cells.size());

    int nodes_used = 0;
    int bonus_cells = 0;
    int bombs_in_path = 0;
    int bfs_token = 0;

    auto push_cell = [&](int idx) {
        if (cover[idx] > 0) --bonus_cells;
        visited[idx] = 1;
        path.push_back(idx);

        if (!bomb_level) return;

        int r = idx / N;
        int c = idx % N;
        if (!board.at(r, c).is_bomb()) return;

        ++bombs_in_path;
        for (int dr = -1; dr <= 1; ++dr) {
            for (int dc = -1; dc <= 1; ++dc) {
                int nr = r + dr, nc = c + dc;
                if (!board.in_bounds(nr, nc)) continue;
                int ni = nr * N + nc;
                if (!visited[ni] && cover[ni] == 0) ++bonus_cells;
                ++cover[ni];
            }
        }
    };

    auto pop_cell = [&](int idx) {
        if (bomb_level) {
            int r = idx / N;
            int c = idx % N;
            if (board.at(r, c).is_bomb()) {
                for (int dr = -1; dr <= 1; ++dr) {
                    for (int dc = -1; dc <= 1; ++dc) {
                        int nr = r + dr, nc = c + dc;
                        if (!board.in_bounds(nr, nc)) continue;
                        int ni = nr * N + nc;
                        if (!visited[ni] && cover[ni] == 1) --bonus_cells;
                        --cover[ni];
                    }
                }
                --bombs_in_path;
            }
        }

        visited[idx] = 0;
        if (cover[idx] > 0) ++bonus_cells;
        path.pop_back();
    };

    auto reachable_from_end = [&](int start_idx) {
        ++bfs_token;
        if (bfs_token == std::numeric_limits<int>::max()) {
            std::fill(bfs_mark.begin(), bfs_mark.end(), 0);
            bfs_token = 1;
        }

        std::array<int, 144> q{};
        int head = 0, tail = 0;
        q[tail++] = start_idx;
        bfs_mark[start_idx] = bfs_token;
        int count = 0;

        while (head < tail) {
            int cur = q[head++];
            ++count;
            int r = cur / N;
            int c = cur % N;
            for (int d = 0; d < 4; ++d) {
                int nr = r + DR[d], nc = c + DC[d];
                if (!board.in_bounds(nr, nc)) continue;
                int ni = nr * N + nc;
                if (!in_comp[ni]) continue;
                if (visited[ni] && ni != start_idx) continue;
                if (bfs_mark[ni] == bfs_token) continue;
                bfs_mark[ni] = bfs_token;
                q[tail++] = ni;
            }
        }
        return count;
    };

    auto dfs = [&](auto& self, int cur_idx) -> void {
        if (nodes_used >= node_budget) return;
        ++nodes_used;

        int len = static_cast<int>(path.size());
        if (len >= 2) {
            int score = path_score(len) + 10 * bonus_cells;
            pool.add_from_indices(path, N, score, bombs_in_path);
        }

        if (nodes_used >= node_budget || len == static_cast<int>(comp.cells.size())) return;

        if (pool.full()) {
            int reachable = reachable_from_end(cur_idx);
            int optimistic_len = len + reachable - 1;
            int optimistic_bonus = bonus_cells;
            if (bomb_level) {
                int remain_bombs = comp.bomb_count - bombs_in_path;
                optimistic_bonus = std::min(board.N * board.N - 2, bonus_cells + 8 * remain_bombs);
            }
            int optimistic_score = path_score(optimistic_len) + 10 * optimistic_bonus;
            if (optimistic_score <= pool.worst_score()) return;
        }

        struct NextStep {
            int priority;
            int rem_deg;
            int idx;
        };
        std::array<NextStep, 4> nexts{};
        int next_count = 0;

        int r = cur_idx / N;
        int c = cur_idx % N;
        for (int d = 0; d < 4; ++d) {
            int nr = r + DR[d], nc = c + DC[d];
            if (!board.in_bounds(nr, nc)) continue;
            int ni = nr * N + nc;
            if (!in_comp[ni] || visited[ni]) continue;

            int rem_deg = 0;
            for (int dd = 0; dd < 4; ++dd) {
                int nnr = nr + DR[dd], nnc = nc + DC[dd];
                if (!board.in_bounds(nnr, nnc)) continue;
                int nni = nnr * N + nnc;
                if (in_comp[nni] && !visited[nni]) ++rem_deg;
            }

            int priority = rem_deg * 100;
            if (bomb_level && board.at(nr, nc).is_bomb()) priority -= 35;
            if (board.at(nr, nc).is_wildcard()) priority -= 12;
            priority += std::min({nr, nc, N - 1 - nr, N - 1 - nc});
            nexts[next_count++] = {priority, rem_deg, ni};
        }

        for (int i = 1; i < next_count; ++i) {
            NextStep key = nexts[i];
            int j = i - 1;
            while (j >= 0) {
                bool should_shift = false;
                if (nexts[j].priority != key.priority) {
                    should_shift = nexts[j].priority > key.priority;
                } else {
                    should_shift = nexts[j].rem_deg > key.rem_deg;
                }
                if (!should_shift) break;
                nexts[j + 1] = nexts[j];
                --j;
            }
            nexts[j + 1] = key;
        }

        for (int i = 0; i < next_count; ++i) {
            push_cell(nexts[i].idx);
            self(self, nexts[i].idx);
            pop_cell(nexts[i].idx);
            if (nodes_used >= node_budget) return;
        }
    };

    for (int start_idx : starts) {
        if (nodes_used >= node_budget) break;
        if (pool.full() && comp.optimistic_score <= pool.worst_score()) break;
        push_cell(start_idx);
        dfs(dfs, start_idx);
        pop_cell(start_idx);
    }

    result.candidates = pool.items();
    result.nodes_used = nodes_used;
    return result;
}

static std::vector<MoveCandidate> generate_candidates(const Board& board, const CandidateGenConfig& cfg) {
    std::vector<MoveCandidate> empty;
    if (cfg.total_budget <= 0 || cfg.candidate_limit <= 0) return empty;

    std::vector<ComponentInfo> comps = collect_components(board);
    if (comps.empty()) return empty;

    CandidatePool global_pool(cfg.candidate_limit);
    int remaining_budget = cfg.total_budget;

    for (const ComponentInfo& comp : comps) {
        if (remaining_budget <= 0) break;
        if (global_pool.full() && comp.optimistic_score <= global_pool.worst_score()) continue;

        int component_budget = cfg.component_budget_base +
                               cfg.component_budget_per_cell * static_cast<int>(comp.cells.size());
        component_budget = std::min(component_budget, remaining_budget);
        if (component_budget <= 0) break;

        ComponentSearchResult local = search_component(
            board,
            comp,
            component_budget,
            std::max(1, std::min(cfg.start_limit, static_cast<int>(comp.cells.size()))),
            cfg.per_component_limit
        );

        remaining_budget -= local.nodes_used;
        for (const MoveCandidate& cand : local.candidates) {
            global_pool.add_candidate(cand, board.N);
        }
    }

    return global_pool.items();
}

static int estimate_future_score(const Board& board,
                                 const std::vector<SearchStage>& stages,
                                 int stage_idx) {
    if (stage_idx >= static_cast<int>(stages.size()) || board.is_deadlocked()) return 0;

    const SearchStage& stage = stages[stage_idx];
    std::vector<MoveCandidate> cands = generate_candidates(board, stage.gen);
    if (cands.empty()) return 0;

    int branch = std::min(stage.branch_width, static_cast<int>(cands.size()));
    int best = 0;

    for (int i = 0; i < branch; ++i) {
        const MoveCandidate& cand = cands[i];
        Board next_board = board.preview(cand.path);
        int future = estimate_future_score(next_board, stages, stage_idx + 1);

        int total = static_cast<int>(std::lround(stage.immediate_weight * cand.score));
        if (stage_idx + 1 < static_cast<int>(stages.size())) {
            total += static_cast<int>(std::lround(stage.future_weight * future));
        }
        if (next_board.is_deadlocked()) total -= stage.deadlock_penalty;
        best = std::max(best, total);
    }

    return best;
}

static std::vector<SearchStage> make_search_plan(const Board& board) {
    if (board.N == 12) {
        return {
            {{165000, 4300, 1850, 20, 6, 10}, 7, 70, 0.42, 1.28},
            {{ 38000, 1300,  720,  8, 3,  7}, 3, 35, 0.30, 1.14},
            {{ 10000,  450,  250,  4, 2,  5}, 1,  0, 0.00, 1.00},
        };
    }

    if (board.level <= 2) {
        return {
            {{280000, 5800, 2800, 28, 6, 14}, 16, 70, 0.88, 1.02},
            {{ 80000, 2100, 1100, 12, 4,  9},  5, 35, 0.60, 1.00},
            {{ 20000,  800,  500,  6, 2,  7},  1,  0, 0.00, 1.00},
        };
    }

    if (board.level == 3) {
        return {
            {{220000, 5000, 2400, 26, 7, 13}, 12, 70, 0.62, 1.08},
            {{ 65000, 1800, 1000, 10, 4,  8},  4, 35, 0.40, 1.02},
            {{ 16000,  650,  380,  5, 2,  6},  1,  0, 0.00, 1.00},
        };
    }

    if (board.level == 4) {
        return {
            {{180000, 4300, 2000, 22, 6, 12},  8, 70, 0.45, 1.22},
            {{ 45000, 1400,  800,  8, 3,  7},  3, 35, 0.25, 1.10},
            {{ 12000,  500,  280,  4, 2,  5},  1,  0, 0.00, 1.00},
        };
    }

    return {
        {{190000, 4200, 2100, 24, 6, 12}, 10, 70, 0.62, 1.12},
        {{ 60000, 1800,  950, 10, 4,  8},  4, 35, 0.40, 1.04},
        {{ 15000,  600,  350,  5, 2,  6},  1,  0, 0.00, 1.00},
    };
}

static std::vector<std::pair<int,int>> fallback_path(const Board& board) {
    for (int r = 0; r < board.N; ++r) {
        for (int c = 0; c < board.N; ++c) {
            int color1 = board.at(r, c).color();
            if (c + 1 < board.N) {
                int color2 = board.at(r, c + 1).color();
                if (color1 == color2 || color1 == 0 || color2 == 0) {
                    return {{r, c}, {r, c + 1}};
                }
            }
            if (r + 1 < board.N) {
                int color2 = board.at(r + 1, c).color();
                if (color1 == color2 || color1 == 0 || color2 == 0) {
                    return {{r, c}, {r + 1, c}};
                }
            }
        }
    }
    return {{0, 0}, {0, 1}};
}

std::vector<std::pair<int,int>> find_best_path(const Board& board) {
    std::vector<SearchStage> plan = make_search_plan(board);
    std::vector<MoveCandidate> roots = generate_candidates(board, plan.front().gen);
    if (roots.empty()) return fallback_path(board);

    int branch = std::min(plan.front().branch_width, static_cast<int>(roots.size()));
    int best_total = std::numeric_limits<int>::min();
    std::vector<std::pair<int,int>> best_path = roots.front().path;

    for (int i = 0; i < branch; ++i) {
        const MoveCandidate& cand = roots[i];
        Board next_board = board.preview(cand.path);
        int future = estimate_future_score(next_board, plan, 1);

        int total = static_cast<int>(std::lround(plan.front().immediate_weight * cand.score));
        if (plan.size() > 1) {
            total += static_cast<int>(std::lround(plan.front().future_weight * future));
        }
        if (next_board.is_deadlocked()) total -= plan.front().deadlock_penalty;

        if (total > best_total) {
            best_total = total;
            best_path = cand.path;
        }
    }

    return best_path;
}

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);

    GameController ctl;
    while (ctl.update()) {
        auto path = find_best_path(ctl.board());
        ctl.respond(path);
    }
    return 0;
}
