import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { clientID } from './utils';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {

  constructor() {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = request.headers.get('x-access-token') || localStorage.getItem('auth_token');
    request = request.clone({
      setHeaders: {
        'clientid': `${clientID()}`,
        'x-access-token': `${token}`
      }
    });  

    return next.handle(request);
  }

  
}
