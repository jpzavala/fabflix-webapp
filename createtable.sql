DROP DATABASE IF EXISTS moviedb;
CREATE DATABASE moviedb;
USE moviedb;

-- movies
CREATE TABLE IF NOT EXISTS movies (
id VARCHAR(10) DEFAULT '',
title VARCHAR(100) NOT NULL DEFAULT '',
`year` INT NOT NULL,
director VARCHAR(100) NOT NULL DEFAULT '',
PRIMARY KEY (id)
);

-- stars
CREATE TABLE IF NOT EXISTS stars (
id VARCHAR(10) DEFAULT '',
`name` VARCHAR(100) NOT NULL DEFAULT '',
birthYear INT,
PRIMARY KEY (id)
);

-- starts in movies
CREATE TABLE IF NOT EXISTS stars_in_movies (
starId VARCHAR(10) DEFAULT '',
movieId VARCHAR(10) DEFAULT '',
PRIMARY KEY (starId, movieId),
FOREIGN KEY (starId) REFERENCES stars (id) ON DELETE CASCADE,
FOREIGN KEY (movieId) REFERENCES movies (id) ON DELETE CASCADE
);

-- genres
CREATE TABLE IF NOT EXISTS genres (
id INT AUTO_INCREMENT,
`name` VARCHAR(32) NOT NULL DEFAULT '',
PRIMARY KEY (id)
);

-- genres_in_movies
CREATE TABLE IF NOT EXISTS genres_in_movies (
genreId INT,
movieId VARCHAR(10) DEFAULT '',
PRIMARY KEY (genreId, movieId),
FOREIGN KEY (genreId) REFERENCES genres (id) ON DELETE CASCADE,
FOREIGN KEY (movieId) REFERENCES movies (id) ON DELETE CASCADE
);

-- creditcards (written here since it is referenced after)
CREATE TABLE IF NOT EXISTS creditcards (
id VARCHAR(20) DEFAULT '',
firstName VARCHAR(50) NOT NULL DEFAULT '',
lastName VARCHAR(50) NOT NULL DEFAULT '',
expiration DATE NOT NULL,
PRIMARY KEY (id)
);

-- customers
CREATE TABLE IF NOT EXISTS customers (
id INT AUTO_INCREMENT,
firstName VARCHAR(50) NOT NULL DEFAULT '',
lastName VARCHAR(50) NOT NULL DEFAULT '',
ccId VARCHAR(20) NOT NULL DEFAULT '',
address VARCHAR(200) NOT NULL DEFAULT '',
email VARCHAR(50) NOT NULL DEFAULT '',
`password` VARCHAR(20) NOT NULL DEFAULT '',
PRIMARY KEY (id),
FOREIGN KEY (ccId) REFERENCES creditcards (id) ON DELETE CASCADE
);

-- sales
CREATE TABLE IF NOT EXISTS sales (
id INT AUTO_INCREMENT,
customerId INT NOT NULL,
movieId VARCHAR(10) NOT NULL DEFAULT '',
saleDate DATE NOT NULL,
PRIMARY KEY (id),
FOREIGN KEY (customerId) references customers (id) ON DELETE CASCADE,
FOREIGN KEY (movieId) references movies (id) ON DELETE CASCADE
);

-- ratings
CREATE TABLE IF NOT EXISTS ratings (
movieId VARCHAR(10) DEFAULT '',
rating FLOAT NOT NULL,
numVotes INT NOT NULL,
PRIMARY KEY (movieId),
FOREIGN KEY (movieId) REFERENCES movies (id) ON DELETE CASCADE
);