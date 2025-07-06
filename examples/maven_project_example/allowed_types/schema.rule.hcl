predicate "column" "has_default_or_nullable" {
  or {
    null {
      eq = true
    }
    default {
      ne = null
    }
  }
}

rule "schema" "no-null-without-default" {
  description = "NOT NULL columns should have default values"
  table {
    column {
      assert {
        predicate = predicate.column.has_default_or_nullable
        message = "Column ${self.name} is NOT NULL but has no default value"
      }
    }
  }
}
