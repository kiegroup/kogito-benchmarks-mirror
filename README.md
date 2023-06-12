# Kogito Benchmarks [![PR Check](https://github.com/kiegroup/kogito-benchmarks/actions/workflows/pull_request.yml/badge.svg "helloooooooooo")](https://github.com/kiegroup/kogito-benchmarks/actions/workflows/pull_request.yml)

Bare-metal ("2nd-level") benchmarks for Kogito Decision (DMN), Prediction (PMML) and Process (BPMN) Services covering different scenarios (load testing, memory usage, start/stop duration…) on Quarkus and Spring Boot.

Repository consists of 2 modules:
* kogito-benchmarks-framework - framework itself
* kogito-benchmarks-tests - tests which use the first module as a dependency

The framework is based on the [quarkus-qe/quarkus-startstop](https://github.com/quarkus-qe/quarkus-startstop) framework but adds support for Spring Boot.
