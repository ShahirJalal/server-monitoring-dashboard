import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {

  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('starts logged out', () => {
    expect(service.isLoggedIn).toBeFalse();
  });

  it('login sets currentUser on success', () => {

    service.login('admin', 'admin123').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ username: 'admin' });

    expect(service.isLoggedIn).toBeTrue();
    expect(service.currentUser?.username).toBe('admin');
  });

  it('restoreSession clears currentUser on 401', () => {

    service.restoreSession().subscribe(user => expect(user).toBeNull());

    const req = httpMock.expectOne('/api/auth/me');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(service.isLoggedIn).toBeFalse();
  });

  it('logout clears currentUser', () => {

    service.login('admin', 'admin123').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ username: 'admin' });
    expect(service.isLoggedIn).toBeTrue();

    service.logout().subscribe();
    httpMock.expectOne('/api/auth/logout').flush(null);

    expect(service.isLoggedIn).toBeFalse();
  });
});
