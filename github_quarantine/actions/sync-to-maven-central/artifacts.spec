{
  "files": [
    {
      "aql": {
        "items.find": {
          "$and": [
            {
              "@build.name": "${buildName}",
              "@build.number": "${buildNumber}",
              "path": {
                "$nmatch": "io.github.kotlinmania.spring.boot.spring-boot-docs/*"
              }
            }
          ]
        }
      },
      "target": "nexus/"
    }
  ]
}
