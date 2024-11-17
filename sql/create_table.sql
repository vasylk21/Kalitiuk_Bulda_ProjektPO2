CREATE TABLE Users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rating FLOAT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Cards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    color VARCHAR(20),
    type VARCHAR(20),
    value INT DEFAULT 0
);

CREATE TABLE Games (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    winner_id INT,
    FOREIGN KEY (winner_id) REFERENCES Users(id)
);

CREATE TABLE GamePlayers (
    game_id INT NOT NULL,
    user_id INT NOT NULL,
    position INT,
    FOREIGN KEY (game_id) REFERENCES Games(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    PRIMARY KEY (game_id, user_id)
);

CREATE TABLE GameCards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    card_id INT NOT NULL,
    user_id INT NOT NULL,
    turn_order INT,
    FOREIGN KEY (game_id) REFERENCES Games(id),
    FOREIGN KEY (card_id) REFERENCES Cards(id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);
