data "external_schema" "hibernate" {
  program = [
     "mvn",
     "-q",
     "compile",
     "hibernate-provider:schema"
  ]
}

data "external_schema" "hibernate_postgresql" {
  program = [
     "mvn",
     "-q",
     "compile",
     "hibernate-provider:schema",
     "-Dproperties=postgresql.properties"
  ]
}

env "hibernate" {
   src = data.external_schema.hibernate.url
   dev = "docker://mysql/8/dev"
   migration {
      dir = "file://migrations"
   }
   format {
      migrate {
         diff = "{{ sql . \"  \" }}"
      }
   }
}

env "hibernate_postgresql" {
   src = data.external_schema.hibernate_postgresql.url
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
