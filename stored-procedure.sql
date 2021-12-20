USE moviedb;
DELIMITER $$

CREATE PROCEDURE add_movie(
IN _id VARCHAR(10), IN _title VARCHAR(100), IN _year INT, IN _director VARCHAR(100), IN _starid VARCHAR(100), IN _name VARCHAR(25), IN _genre VARCHAR(50))
add_movie: BEGIN
	IF _title = '' OR _year = '' OR _director = '' THEN
		SELECT -1;
        LEAVE add_movie;
	END IF;
    
    INSERT INTO movies(id, title, `year`, director) VALUES (_id, _title, _year, _director);
    
    IF _name != '' THEN
		IF _name NOT IN (SELECT `name` FROM stars) THEN
			INSERT INTO stars (id, `name`) VALUES (_starid, _name);
		END IF;
        INSERT INTO stars_in_movies (starId, movieId)
        SELECT (SELECT id FROM stars WHERE `name` = _name LIMIT 1) AS starId, (SELECT id FROM movies WHERE title = _title LIMIT 1) AS movieId;
    END IF;
    
    IF _genre != '' THEN
		IF _genre NOT IN (SELECT `name` FROM genres) THEN
			INSERT INTO genres (`name`) VALUES (_genre);
		END IF;
		
		INSERT INTO genres_in_movies (genreId, movieId)
		SELECT (SELECT id FROM genres WHERE `name` = _genre LIMIT 1) AS genreId, (SELECT id FROM movies WHERE title = _title LIMIT 1) AS movieId;
	END IF;
    SELECT 1;
END
$$

DELIMITER ;