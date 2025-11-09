window.swaggerSpec={
  "openapi" : "3.0.1",
  "info" : {
    "title" : "Tutorials API",
    "description" : "SpringRestDocs + Swagger tutorial",
    "version" : "0.0.1"
  },
  "servers" : [ {
    "url" : "http://localhost:8080"
  } ],
  "tags" : [ ],
  "paths" : {
    "/users" : {
      "post" : {
        "tags" : [ "users" ],
        "operationId" : "users/register",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/users1644608199"
              },
              "examples" : {
                "users/register" : {
                  "value" : "{\n  \"username\" : \"test\",\n  \"password\" : \"password\"\n}"
                }
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "200",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/users1126388308"
                },
                "examples" : {
                  "users/register" : {
                    "value" : "{\n  \"id\" : \"tutorials-5673d7c873c1404a80eb3cf7dc23269d\",\n  \"username\" : \"test\"\n}"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/users/{id}" : {
      "get" : {
        "tags" : [ "users" ],
        "operationId" : "users/detail",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "description" : "User ID",
          "required" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "200",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/users-id-2029229566"
                },
                "examples" : {
                  "users/detail" : {
                    "value" : "{\n  \"id\" : \"tutorials-3d7dd663b29144c6841d5149cca36b63\",\n  \"username\" : \"test\",\n  \"nickname\" : null,\n  \"birthDate\" : null,\n  \"gender\" : \"UNKNOWN\"\n}"
                  }
                }
              }
            }
          }
        }
      },
      "put" : {
        "tags" : [ "users" ],
        "operationId" : "users/update",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "description" : "User ID",
          "required" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/users-id-1928936603"
              },
              "examples" : {
                "users/update" : {
                  "value" : "{\n  \"nickname\" : \"nickname\",\n  \"birthDate\" : \"2020-11-01\",\n  \"gender\" : \"MALE\"\n}"
                }
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "200"
          }
        }
      }
    }
  },
  "components" : {
    "schemas" : {
      "users1644608199" : {
        "required" : [ "password", "username" ],
        "type" : "object",
        "properties" : {
          "password" : {
            "type" : "string",
            "description" : "Password"
          },
          "username" : {
            "type" : "string",
            "description" : "User ID"
          }
        }
      },
      "users-id-1928936603" : {
        "type" : "object",
        "properties" : {
          "gender" : {
            "type" : "string",
            "description" : "link:enums/Gender.html[Gender,role=\"popup\"]",
            "nullable" : true
          },
          "nickname" : {
            "type" : "string",
            "description" : "Nickname",
            "nullable" : true
          },
          "birthDate" : {
            "type" : "string",
            "description" : "Birth Date",
            "nullable" : true
          }
        }
      },
      "users1126388308" : {
        "required" : [ "id", "username" ],
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string",
            "description" : "User ID"
          },
          "username" : {
            "type" : "string",
            "description" : "Username"
          }
        }
      },
      "users-id-2029229566" : {
        "required" : [ "id", "username" ],
        "type" : "object",
        "properties" : {
          "gender" : {
            "type" : "string",
            "description" : "link:enums/Gender.html[Gender,role=\"popup\"]",
            "nullable" : true
          },
          "nickname" : {
            "type" : "string",
            "description" : "Nickname",
            "nullable" : true
          },
          "id" : {
            "type" : "string",
            "description" : "User ID"
          },
          "birthDate" : {
            "type" : "number",
            "description" : "Birth Date",
            "nullable" : true
          },
          "username" : {
            "type" : "string",
            "description" : "Username"
          }
        }
      }
    }
  }
}