-- Create "movies" table
CREATE TABLE "movies" (
  "id" bigserial NOT NULL,
  "numberinseries" integer NULL,
  "title" character varying(255) NULL,
  PRIMARY KEY ("id")
);
-- Create "actors" table
CREATE TABLE "actors" (
  "name" character varying(255) NOT NULL,
  PRIMARY KEY ("name")
);
-- Create "movieparticipation" table
CREATE TABLE "movieparticipation" (
  "actorname" character varying(255) NOT NULL,
  "movieid" bigint NOT NULL,
  PRIMARY KEY ("actorname", "movieid"),
  CONSTRAINT "fkaq2kkwvh9870847sm35vtjtiy" FOREIGN KEY ("movieid") REFERENCES "movies" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fktm8fbwa577lnbvwdjegwxvget" FOREIGN KEY ("actorname") REFERENCES "actors" ("name") ON UPDATE NO ACTION ON DELETE NO ACTION
);
