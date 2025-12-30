-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'PLAYER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- Magic tokens for passwordless login
CREATE TABLE magic_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_magic_tokens_token ON magic_tokens(token);
CREATE INDEX idx_magic_tokens_user_id ON magic_tokens(user_id);

-- Quiz rounds
CREATE TABLE rounds (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rounds_active ON rounds(active);

-- Questions
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    round_id BIGINT NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    text TEXT,
    image_filename VARCHAR(255),
    explanation TEXT
);

CREATE INDEX idx_questions_round_id ON questions(round_id);

-- Answer options
CREATE TABLE answer_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    text TEXT,
    image_filename VARCHAR(255),
    correct BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_answer_options_question_id ON answer_options(question_id);

-- Player round participation
CREATE TABLE player_rounds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    round_id BIGINT NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    total_score INT NOT NULL DEFAULT 0,
    UNIQUE(user_id, round_id)
);

CREATE INDEX idx_player_rounds_user_id ON player_rounds(user_id);
CREATE INDEX idx_player_rounds_round_id ON player_rounds(round_id);
CREATE INDEX idx_player_rounds_total_score ON player_rounds(total_score DESC);

-- Player answers
CREATE TABLE player_answers (
    id BIGSERIAL PRIMARY KEY,
    player_round_id BIGINT NOT NULL REFERENCES player_rounds(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    answer_id BIGINT REFERENCES answer_options(id) ON DELETE SET NULL,
    question_shown_at TIMESTAMP WITH TIME ZONE NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    score INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_player_answers_player_round_id ON player_answers(player_round_id);
CREATE INDEX idx_player_answers_question_id ON player_answers(question_id);
