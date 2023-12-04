data "external_schema" "hibernate" {
  program = [
     "./gradlew",
     "-q",
     "schema",
     "--properties",
     "postgresql.properties"
  ]
}

env "hibernate" {
   src = data.external_schema.hibernate.url
   dev = "docker://postgres/15/dev"
   migration {
      dir = "file://migrations_postgresql"
   }
   format {
      migrate {
         diff = "{{ sql . \"  \" }}"
      }
   }
}
