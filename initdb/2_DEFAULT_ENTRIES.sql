INSERT INTO majors (id, name, description) VALUES (1, 'MIN', 'Ouaiiis du code partout');

INSERT INTO students (id, first_name, last_name, birthdate, major_id, image) VALUES (1, 'Paul', 'Harrohide', '2002-06-15', 1, null);
INSERT INTO students (id, first_name, last_name, birthdate, major_id, image) VALUES (2, 'Jean', 'Bonbeur', '2001-08-21', 1, null);
INSERT INTO students (id, first_name, last_name, birthdate, major_id, image) VALUES (3, 'Alain', 'Térieur', '2000-01-11', 1, null);

INSERT INTO courses (id, name, hours) VALUES (1, 'Spanish', 30);
INSERT INTO courses (id, name, hours) VALUES (2, 'German', 30);
INSERT INTO courses (id, name, hours) VALUES (3, 'Internet of Things', 30);
INSERT INTO courses (id, name, hours) VALUES (4, 'Thermodynamic', 30);
INSERT INTO courses (id, name, hours) VALUES (5, 'Anatomy', 30);
INSERT INTO courses (id, name, hours) VALUES (6, 'Maths', 30);
INSERT INTO courses (id, name, hours) VALUES (7, 'Java', 30);
INSERT INTO courses (id, name, hours) VALUES (8, 'Lean Management', 30);
INSERT INTO student_course (id, student_id, course_id) VALUES (1, 1, 7);


