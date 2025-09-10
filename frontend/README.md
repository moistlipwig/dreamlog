# DreamlogFrontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.2.2.

Requires Node.js 20.19.0, 22.12.0, or 24.x to match Angular's supported engines.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

Unit tests are executed with [Jest](https://jestjs.io/). To run them locally, use:

```bash
npm test
```

For continuous integration run:

```bash
npm run test:ci
```

## End-to-end tests

End-to-end (e2e) tests are maintained in a dedicated project outside this frontend codebase.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Architecture & UX notes

- Standalone Angular 20 components with Angular Material and TailwindCSS.
- Core services (`AuthService`, `DreamsService`, `SearchService`, `CalendarService`) expose reactive `BehaviorSubject` state.
- Routing guarded by `authGuard`; forms use `pendingChangesGuard` to warn about unsaved data.
- `AppShell` provides a Material toolbar with a debounced global search bar.
- Shared UI building blocks include `TagChips`, `EmptyState`, and a reusable `ConfirmDialog`.
