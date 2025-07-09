data "external_schema" "hibernate" {
  program = ["./gradlew", "-q", "schema"]
}

data "external_schema" "hibernate_postgresql" {
  program = ["./gradlew", "-q", "schema", "--properties", "postgresql.properties"]
}

env "hibernate" {
  url = data.external_schema.hibernate.url
  dev = "docker://mysql/8/dev"
  src = data.external_schema.hibernate.url
  migration {
    dir = "file://migrations"
  }
  lint {
    rule "hcl" "custom-rules" {
      src = ["schema.rule.hcl"]
    }
  }
}

env "hibernate_postgresql" {
  url = data.external_schema.hibernate_postgresql.url
  dev = "docker://postgres/15/dev?search_path=public"
}

data "external_schema" "hibernate_example" {
   program = [
       "./gradlew",
       "-q",
       "schema"
   ]
}

env "hibernate_example" {
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
