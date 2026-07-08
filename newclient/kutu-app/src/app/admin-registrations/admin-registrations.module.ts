import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminRegistrationsPage } from './admin-registrations.page';

const routes: Routes = [{ path: '', component: AdminRegistrationsPage }];

@NgModule({
  declarations: [AdminRegistrationsPage],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class AdminRegistrationsPageModule {}
