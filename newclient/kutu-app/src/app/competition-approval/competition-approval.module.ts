import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { RouterModule, Routes } from '@angular/router';
import { CompetitionApprovalPage } from './competition-approval.page';

const routes: Routes = [{ path: '', component: CompetitionApprovalPage }];

@NgModule({
  // TermsModalComponent wird einmalig im AppModule deklariert und nur dynamisch geoeffnet.
  declarations: [CompetitionApprovalPage],
  imports: [CommonModule, FormsModule, IonicModule, RouterModule.forChild(routes)],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class CompetitionApprovalPageModule {}
