USE notifyflow_course;
DROP TABLE IF EXISTS deadlock_demo;
CREATE TABLE deadlock_demo (
    id INT NOT NULL PRIMARY KEY,
    value INT NOT NULL
) ENGINE = InnoDB;
INSERT INTO deadlock_demo(id, value) VALUES (1, 0), (2, 0);

