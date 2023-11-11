import { Injectable } from '@angular/core';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class StationGuardService  {

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
