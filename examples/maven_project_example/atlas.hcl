data "external_schema" "hibernate" {
  program = ["mvn", "-q", "hibernate-provider:schema"]
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
