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


CREATE TABLE hierarchy_test_table
(
    id integer NOT NULL,
    uuid uuid NOT NULL,
    name text NOT NULL,
    parent_id integer,
    CONSTRAINT hierarchy_table_pkey PRIMARY KEY (id)
);

ALTER TABLE hierarchy_test_table ADD CONSTRAINT fk_parent
    FOREIGN KEY (parent_id) references hierarchy_test_table(id);

INSERT INTO public.hierarchy_test_table(
    id, uuid, name, parent_id)
VALUES 
   (1, '7e0c2c3a-8113-427c-b8ed-83247a82ba43', 'a1', NULL), 
   (2, 'c779eea7-3e6b-42a2-9ce6-88ca66857dad', 'a2', 1),
   (3, 'cdeec1e6-9e24-477a-a598-e32af0447d43', 'a3', 1),
   (4, '42cb7ad4-6600-4067-90f2-87d81e045172', 'a4', 2),
   (5, '61a75686-a964-451a-86de-b15a89cbcebd', 'a5', 2),
   (6, 'ffe87eae-5a10-44d2-8beb-cd2db7b116b9', 'a6', NULL);
