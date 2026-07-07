import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule, Routes } from '@angular/router';
import { SecurityPage } from './security.page';

const routes: Routes = [{ path: '', component: SecurityPage }];

@NgModule({
  declarations: [SecurityPage],
  imports: [CommonModule, IonicModule, RouterModule.forChild(routes)]
})
export class SecurityPageModule {}
