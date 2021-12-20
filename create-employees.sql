USE moviedb;

-- employees
CREATE TABLE IF NOT EXISTS employees (
email varchar(50) primary key,
password varchar(20) not null,
fullname varchar(100)
);

-- After creating table run:
-- INSERT INTO employees (email, password, fullname) VALUES ('classta@email.edu', 'classta', 'TA CS122B');
-- Encrypt the employee password using java program