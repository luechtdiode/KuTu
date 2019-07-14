import { Injectable } from '@angular/core';
import { Router, ActivatedRouteSnapshot, CanActivate } from '@angular/router';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class StationGuardService implements CanActivate {

  constructor(private router: Router, public backendService: BackendService) {

  }

  canActivate(route: ActivatedRouteSnapshot): boolean {

      if (!(this.backendService.geraet && this.backendService.step)) {
          this.router.navigate(['home']);
          return false;
      }

      return true;

  }

}
