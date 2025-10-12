# DreamLog Frontend - Angular Guidelines

> **Note:** For general project context, architecture, and backend guidelines, see: `../../.claude/CLAUDE.md`
>
> This file contains **frontend-specific** Angular best practices and patterns for the DreamLog project.

---

## Table of Contents

1. [Angular 20 Best Practices](#angular-20-best-practices)
2. [TypeScript Standards](#typescript-standards)
3. [Component Patterns](#component-patterns)
4. [State Management](#state-management)
5. [Testing with Jest](#testing-with-jest)
6. [Styling (Material + Tailwind)](#styling-material--tailwind)
7. [Project-Specific Patterns](#project-specific-patterns)
8. [Token Optimization](#token-optimization)

---

## Angular 20 Best Practices

### Standalone Components (Required)

**All components MUST be standalone.** No NgModules allowed.

### Change Detection Strategy (Required)

**Always use OnPush:**
Benefits:

- Better performance
- Predictable change detection
- Forces immutable patterns with signals

### Native Control Flow (Required)

Use Angular 20's native control flow syntax:

### Dependency Injection

Use `inject()` function instead of constructor injection:

### Inputs and Outputs

Use `input()` and `output()` functions:

---

## TypeScript Standards

### Type Inference

Prefer type inference when obvious:

### Interfaces vs Types

- Use `interface` for object shapes
- Use `type` for unions, intersections, and utilities

---

### Component Structure

Order within component class:

1. Injected dependencies (private)
2. Signals (public state)
3. Inputs (via `input()`)
4. Outputs (via `output()`)
5. Computed values
6. Lifecycle hooks
7. Public methods
8. Private methods

### Host Bindings

Use `host` object instead of decorators:

```typescript
@Component({
  // ✅ CORRECT
  host: {
    '[class.active]': 'isActive()',
    '[attr.role]': '"button"',
    '(click)': 'handleClick()'
  }
})

// ❌ FORBIDDEN
@HostBinding('class.active')
isActive = signal(false);
@HostListener('click')
handleClick()
{
}
```

---

## State Management

### Signals (Primary Mechanism)

Use signals for all component state:

```typescript
export class DreamListComponent {
  // State
  dreams = signal<Dream[]>([]);
  isLoading = signal(false);
  selectedDream = signal<Dream | null>(null);

  // Derived state
  dreamCount = computed(() => this.dreams().length);
  hasDreams = computed(() => this.dreams().length > 0);
  selectedDreamTitle = computed(() =>
    this.selectedDream()?.title ?? 'None'
  );

  // Update state
  addDream(dream: Dream) {
    this.dreams.update(current => [...current, dream]);
  }

  removeDream(id: string) {
    this.dreams.update(current =>
      current.filter(d => d.id !== id)
    );
  }
}
```

### Services for Shared State

For state shared across components:

```typescript

@Injectable({providedIn: 'root'})
export class DreamStateService {
  private dreamsSignal = signal<Dream[]>([]);

  // Public read-only signal
  dreams = this.dreamsSignal.asReadonly();

  // Public computed
  dreamCount = computed(() => this.dreamsSignal().length);

  // Public methods to modify state
  addDream(dream: Dream) {
    this.dreamsSignal.update(current => [...current, dream]);
  }

  setDreams(dreams: Dream[]) {
    this.dreamsSignal.set(dreams);
  }
}
```

### Observables with Signals

Convert observables to signals when needed:

```typescript
export class MyComponent {
  private http = inject(HttpClient);

  data = signal<Data[]>([]);

  ngOnInit() {
    // Option 1: Subscribe and set signal
    this.http.get<Data[]>('/api/data').subscribe(
      data => this.data.set(data)
    );

    // Option 2: Use toSignal (Angular 20)
    this.data = toSignal(
      this.http.get<Data[]>('/api/data'),
      {initialValue: []}
    );
  }
}
```

---

## Testing with Jest

### Component Test Structure

```typescript
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MyComponent} from './my.component';

describe('MyComponent', () => {
  let component: MyComponent;
  let fixture: ComponentFixture<MyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyComponent]  // Standalone component
    }).compileComponents();

    fixture = TestBed.createComponent(MyComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display title', () => {
    component.title.set('Test Title');
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Test Title');
  });

  it('should emit event on button click', () => {
    const spy = jest.fn();
    component.itemClick.subscribe(spy);

    const button = fixture.nativeElement.querySelector('button');
    button.click();

    expect(spy).toHaveBeenCalledWith(expect.any(Object));
  });
});
```

### Service Test Structure

```typescript
import {TestBed} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {DreamService} from './dream.service';

describe('DreamService', () => {
  let service: DreamService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DreamService]
    });

    service = TestBed.inject(DreamService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch dreams', () => {
    const mockDreams = [
      {id: '1', content: 'Dream 1'},
      {id: '2', content: 'Dream 2'}
    ];

    service.getDreams().subscribe(dreams => {
      expect(dreams).toEqual(mockDreams);
      expect(dreams.length).toBe(2);
    });

    const req = httpMock.expectOne('/api/v1/dreams');
    expect(req.request.method).toBe('GET');
    req.flush(mockDreams);
  });
});
```

### Running Tests

```bash
# All tests
npm test

# Watch mode
npm test -- --watch

# Specific file
npm test -- dream.component

# Coverage
npm test -- --coverage

# CI mode
npm run test:ci
```

---

## Styling (Material + Tailwind)

### Component Styling Strategy

1. Use Angular Material components for interactive elements
2. Use TailwindCSS utilities for layout and spacing
3. Keep component-specific styles minimal

### Class and Style Bindings

Use binding syntax instead of directives:

## Project-Specific Patterns

### API Integration

Location: `frontend/src/app/services/`

### Forms (Reactive)

Always use Reactive Forms

## Token Optimization

### Pattern Reference

When creating new components, check existing patterns:

```bash
# Find similar components
Glob: frontend/src/app/components/**/*.component.ts

# Find service pattern
Glob: frontend/src/app/services/*.service.ts

# Find specific implementation
Grep: pattern="inject\(HttpClient\)" path="frontend/src/app"
```

### Common Locations

- **Components:** `frontend/src/app/components/<feature>/`
- **Services:** `frontend/src/app/services/`
- **Models:** `frontend/src/app/models/`
- **Guards:** `frontend/src/app/guards/`
- **Interceptors:** `frontend/src/app/interceptors/`

### Reference Components

Check these for patterns:

- **Landing Page:** `frontend/src/app/components/landing-page/landing-page.component.ts`
- Tests next to components: `*.component.spec.ts`

### Quick Commands

```bash
# Development server
npm start

# Type check only
npm run typecheck

# Run tests
npm test

# Lint and fix
npm run lint:fix

# Format code
npm run format

# Full verification
npm run verify
```

---

## Forbidden Patterns Summary

❌ **DO NOT USE:**

1. `@HostBinding`, `@HostListener` → Use `host` object
2. NgModules → Use standalone components
3. `ngClass`, `ngStyle` → Use `[class]`, `[style]` bindings
4. `*ngIf`, `*ngFor`, `*ngSwitch` → Use `@if`, `@for`, `@switch`
5. `@Input()`, `@Output()` decorators → Use `input()`, `output()` functions
6. `any` type → Use proper types or `unknown`
7. Constructor injection → Use `inject()` function
8. Template-driven forms → Use Reactive Forms
9. Default change detection → Use `OnPush`

---

## Additional Resources

- **Project Guidelines:** [../../.claude/CLAUDE.md](../../.claude/CLAUDE.md)
- **Angular Documentation:** https://angular.dev
- **Material Design:** https://material.angular.io
- **TailwindCSS:** https://tailwindcss.com
- **Jest:** https://jestjs.io
