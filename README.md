
# Import Control Entry Declaration Intervention

The Import Control Entry Declaration Intervention responsibilities:
- receive JSON interventions from C&IT
- persist interventions as XML, and make them available to authenticated users. Message Types:
  - DO NOT LOAD \<CC351> / IE351 [Schema mapping](JSON_XML_MAPPING_INFO.md)

## Development Setup
- Run locally: `sbt run` which runs on port `9812` by default

## Tests
- Run Unit Tests: `sbt test`
- Run Integration Tests: `sbt it:test`

## API

| Path | Supported Methods | Type | Description |
| ----------------------------------------- | ---------------- | -------- |----------- |
|```/```                                    |        GET       | External | Endpoint for users to list unacknowledged interventions for ENS submissions. |
|```/:correlationId```                      |        DELETE    | External | Endpoint for users to fetch an unacknowledged intervention for an ENS submission. |
|```/:correlationId```                      |        POST      | External | Endpoint for users to acknowledge an unacknowledged intervention for an ENS submission. |
|```/import-control/advanced-intervention```|        POST      | Internal | Endpoint for C&IT to return an intervention for an ENS submission. |

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
