import { TestBed } from '@angular/core/testing';
import { ConnectivityService } from './connectivity.service';

describe('ConnectivityService', () => {
  it('exposes the browser online state', () => {
    const service = TestBed.inject(ConnectivityService);
    expect(typeof service.isOnline()).toBe('boolean');
  });
});