import { Injectable } from '@angular/core';
import { Router, ActivatedRouteSnapshot, CanActivate } from '@angular/router';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class VereinsRegistrationGuardService implements CanActivate {

  constructor(
    private router: Router, 
    public backendService: BackendService) 
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
