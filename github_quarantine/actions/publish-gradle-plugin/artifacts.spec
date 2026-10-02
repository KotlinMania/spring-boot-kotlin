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
                "$match": "io.github.kotlinmania.spring.boot.spring-boot-gradle-plugin/*"
              }
            }
          ]
        }
      },
      "target": "repository/"
    }
  ]
}
