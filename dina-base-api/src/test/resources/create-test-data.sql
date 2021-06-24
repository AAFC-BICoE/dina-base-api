-- Table: public.dina_test_table

-- DROP TABLE public.dina_test_table;

/*
 This is a test table for ca.gc.aafc.dina.service.PostgresJsonbService
 PostgresJsonbService can not be tested against H2 in-memory database
 Need to populate a dummy table with some dummy rows
 */
CREATE TABLE public.dina_test_table
(
    id integer NOT NULL,
    name text COLLATE pg_catalog."default" NOT NULL,
    value integer NOT NULL,
    jdata jsonb NOT NULL,
    CONSTRAINT dina_test_table_pkey PRIMARY KEY (id)
)
    WITH (
        OIDS = FALSE
        )
    TABLESPACE pg_default;

INSERT INTO public.dina_test_table(
    id, name, value, jdata)
VALUES (1, 'row_one', 1, '{"attr_01": "val_01"}');

INSERT INTO public.dina_test_table(
    id, name, value, jdata)
VALUES (2, 'row_two', 2, '{"attr_02": "val_02"}');


CREATE TABLE public.hierarchy_test_table
(
    id integer NOT NULL,
    name text NOT NULL,
    parent_id integer,
    FOREIGN KEY (parent_id),
    CONSTRAINT hierarchy_table_pkey PRIMARY KEY (id)

)

INSERT INTO public.hierarchy_test_table(
   id, name, parent_id)
VALUES 
 	(1, 'a1', NULL), 
	(2, 'a2', 1),
	(3, 'a3', 1),
	(4, 'a4', 2),
    (5, 'a5', 2),
    (6, 'a6', NULL);



