-- DEC-2026-1126-class-name-unique: one instructor cannot own two classes with the same name
-- (ignoring case and outer spaces); two instructors may share a name.
CREATE UNIQUE INDEX classes_instructor_name_uk ON identity.classes (instructor_id, lower(btrim(name)));
