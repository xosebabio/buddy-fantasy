import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideApiHttp } from './provide-api-http';

describe('provideApiHttp', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=csrf-token';
    TestBed.configureTestingModule({
      providers: [provideApiHttp(), provideHttpClientTesting(), provideRouter([])],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
  });

  it('copies the CSRF cookie into the header of a write to the API', () => {
    client.post('/api/leagues', { name: 'Peña' }).subscribe();

    const request = http.expectOne('/api/leagues');
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    request.flush({});
  });

  it('does not send the CSRF header to another origin', () => {
    client.put('https://bucket.example/photo', new Blob()).subscribe();

    const request = http.expectOne('https://bucket.example/photo');
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush(null);
  });
});
