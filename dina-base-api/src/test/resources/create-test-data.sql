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
    parent_identifier integer,
    type_uuid uuid,
    CONSTRAINT hierarchy_table_pkey PRIMARY KEY (id)
);

ALTER TABLE hierarchy_test_table ADD CONSTRAINT fk_parent
    FOREIGN KEY (parent_identifier) references hierarchy_test_table(id);

INSERT INTO public.hierarchy_test_table(
    id, uuid, name, parent_identifier, type_uuid)
VALUES 
   (1, '7e0c2c3a-8113-427c-b8ed-83247a82ba43', 'a1', NULL, '02bd39da-5471-4ca2-b82d-b407551c7f31'),
   (2, 'c779eea7-3e6b-42a2-9ce6-88ca66857dad', 'a2', 1, '8d8c6339-4725-4b80-8d04-8e0fa9c5eec8'),
   (3, 'cdeec1e6-9e24-477a-a598-e32af0447d43', 'a3', 1, '8d8c6339-4725-4b80-8d04-8e0fa9c5eec8'),
   (4, '42cb7ad4-6600-4067-90f2-87d81e045172', 'a4', 2, 'bc090507-bcee-48f8-b7bc-4c5551a2c52a'),
   (5, '61a75686-a964-451a-86de-b15a89cbcebd', 'a5', 2, 'bc090507-bcee-48f8-b7bc-4c5551a2c52a'),
   (6, 'ffe87eae-5a10-44d2-8beb-cd2db7b116b9', 'a6', NULL, '02bd39da-5471-4ca2-b82d-b407551c7f31');

CREATE OR REPLACE FUNCTION jsonb_path_exists_varchar(target jsonb, path varchar, vars varchar, caseSensitive boolean)
 RETURNS boolean AS
'
 BEGIN
  IF caseSensitive THEN
    RETURN(jsonb_path_exists(target, path::jsonpath, vars::jsonb));  
  ELSE
    RETURN(jsonb_path_exists(lower(target::text)::jsonb, lower(path::text)::jsonpath, lower(vars::text)::jsonb));
  END IF;
 END
'
LANGUAGE plpgsql STABLE;