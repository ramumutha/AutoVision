import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AvShellComponent } from './shell/av-shell.component';
import { AuthService } from './core/auth/auth.service';

@Component({
  imports: [AvShellComponent],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly auth = inject(AuthService);
}
