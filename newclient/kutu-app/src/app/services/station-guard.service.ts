import { Injectable, inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class StationGuardService  {
  private router = inject(Router);
  backendService = inject(BackendService);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() {

  }

  canActivate(route: ActivatedRouteSnapshot): boolean {

      if (!(this.backendService.geraet && this.backendService.step)) {
          this.router.navigate(['home']);
          return false;
      }

      return true;

  }

}
