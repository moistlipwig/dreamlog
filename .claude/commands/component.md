# Create Angular Component

Generate a new Angular component following project best practices.

## Instructions

1. **Gather Requirements**
  - Component name (kebab-case)
  - Purpose/functionality
  - Parent feature/module
  - Required inputs/outputs

2. **Check for Similar Components**
  - Search: `Glob: frontend/src/app/components/**/*.component.ts`
  - Review similar components for pattern consistency
  - Identify reusable services or utilities

3. **Generate Component**
  - Navigate: `cd frontend`
  - Generate: `ng generate component components/<feature>/<name> --standalone`
  - Or create manually if custom setup needed

4. **Configure Component**

   Follow Angular 20 best practices:

5. **Template Best Practices**
  - Use native control flow: `@if`, `@for`, `@switch`
  - NO structural directives: *ngIf, *ngFor, *ngSwitch
  - Use `[class.name]` instead of `ngClass`
  - Use `[style.property]` instead of `ngStyle`
  - Track items in @for: `@for (item of items(); track item.id)`

6. **Styling**
  - Use TailwindCSS utility classes
  - Leverage Angular Material components
  - Keep component-specific styles minimal
  - Use `@apply` for reusable Tailwind patterns

7. **Create Tests**
  - File: `<name>.component.spec.ts` next to component
  - Use Jest with jest-preset-angular
  - Test template rendering
  - Test signal updates
  - Test input/output behavior

8. **Integration**
  - Import component where needed (standalone imports)
  - Update routing if new route required
  - Add to navigation if applicable

## Example Component

## Checklist

After component creation, verify:

- [ ] Component uses standalone: true
- [ ] ChangeDetectionStrategy.OnPush set
- [ ] Uses signals for state
- [ ] Uses input()/output() functions
- [ ] Template uses @if/@for syntax
- [ ] Test file created and passes
- [ ] Properly imported where used
- [ ] Follows naming conventions
- [ ] Material + Tailwind styled
