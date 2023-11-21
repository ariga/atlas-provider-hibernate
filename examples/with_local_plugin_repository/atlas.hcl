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
}

env "hibernate_postgresql" {
  url = data.external_schema.hibernate_postgresql.url
  dev = "docker://postgres/15/dev?search_path=public"
}

