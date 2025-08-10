import { Injectable, inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class VereinsRegistrationGuardService  {
  private router = inject(Router);
  backendService = inject(BackendService);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() 
    {

  }

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const wkId = route.paramMap.get('wkId');
    const regId = route.paramMap.get('regId');
    if (regId === '0' || this.backendService.loggedIn && this.backendService.authenticatedClubId === regId) {
      return true;
    }
    this.router.navigate(['registration/' + wkId]);
    return false;
  }

}
