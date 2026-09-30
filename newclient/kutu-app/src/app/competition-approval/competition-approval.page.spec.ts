import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { IonicModule, ModalController, ToastController } from '@ionic/angular';
import { of, throwError } from 'rxjs';

import { CompetitionApprovalPage } from './competition-approval.page';
import { AdminBackendService } from '../services/admin-backend.service';

const UUID = 'b3f0a5f2-6a4a-4f7c-9c1e-2f9b0a1c2d3e';
const MAIL = 'organisator@verein.ch';

class AdminBackendStub {
  requests: any[] = [];
  result: any = of({ message: 'EMail x@y.z erfolgreich verifiziert', success: true });
  callCount = 0;

  approveCompetitionByMail(uuid: string, request: any) {
    this.callCount++;
    this.requests.push({ uuid, request });
    return this.result;
  }
}

class ToastStub {
  create = () => Promise.resolve({ present: () => Promise.resolve() });
}

class ModalStub {
  present = () => Promise.resolve();
  onDidDismiss = () => Promise.resolve({ data: undefined });
}

describe('CompetitionApprovalPage', () => {
  let backend: AdminBackendStub;
  let component: CompetitionApprovalPage;

  const build = (params: any, query: any) => {
    backend = new AdminBackendStub();
    TestBed.configureTestingModule({
      declarations: [CompetitionApprovalPage],
      imports: [IonicModule.forRoot(), FormsModule],
      providers: [
        provideZonelessChangeDetection(),
        { provide: AdminBackendService, useValue: backend },
        { provide: ToastController, useValue: new ToastStub() },
        { provide: ModalController, useValue: new ModalStub() },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(params), queryParamMap: convertToParamMap(query) } } }
      ]
    });
    const fixture = TestBed.createComponent(CompetitionApprovalPage);
    component = fixture.componentInstance;
    return fixture;
  };

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('reads uuid from the path and mail from the query', () => {
    build({ uuid: UUID }, { mail: MAIL });
    expect(component.uuid()).toBe(UUID);
    expect(component.mail()).toBe(MAIL);
  });

  it('is invalid with an incomplete link', () => {
    build({}, {});
    expect(component.uuid()).toBeNull();
    expect(component.mail()).toBeNull();
    expect(component.isValid()).toBeFalse();
  });

  it('is invalid while fields or the terms are missing', () => {
    build({ uuid: UUID }, { mail: MAIL });
    expect(component.isValid()).toBeFalse();

    component.creatorName.set('  ');
    component.creatorAddress.set('Strasse 1');
    component.creatorPhone.set('044 000');
    expect(component.isValid()).toBeFalse();

    component.creatorName.set('Hans Muster');
    expect(component.isValid()).toBeFalse();

    component.termsAccepted.set(true);
    expect(component.isValid()).toBeTrue();
  });

  it('trims the values and sends the terms version of the backend', () => {
    build({ uuid: UUID }, { mail: MAIL });
    component.creatorName.set('  Hans Muster  ');
    component.creatorAddress.set(' Musterstrasse 1 ');
    component.creatorPhone.set(' +49 123 ');
    component.termsAccepted.set(true);
    component.termsAcceptedVersion.set('1.0');

    const request = component.request();
    expect(request.mail).toBe(MAIL);
    expect(request.creator).toEqual({
      creatorName: 'Hans Muster',
      creatorAddress: 'Musterstrasse 1',
      creatorPhone: '+49 123',
      termsVersion: '1.0'
    });
  });

  it('posts to the competition endpoint and reports success', async () => {
    build({ uuid: UUID }, { mail: MAIL });
    component.creatorName.set('Hans Muster');
    component.creatorAddress.set('Musterstrasse 1');
    component.creatorPhone.set('+49 123');
    component.termsAccepted.set(true);

    await component.submit();

    expect(backend.callCount).toBe(1);
    expect(backend.requests[0].uuid).toBe(UUID);
    expect(backend.requests[0].request.mail).toBe(MAIL);
    expect(component.submitted()).toBeTrue();
    expect(component.resultSuccess()).toBeTrue();
    expect(component.resultMessage()).toContain('erfolgreich verifiziert');
    expect(component.submitting()).toBeFalse();
  });

  it('does not send an invalid form', async () => {
    build({ uuid: UUID }, { mail: MAIL });
    await component.submit();
    expect(backend.callCount).toBe(0);
  });

  it('does not send twice while a request is running', async () => {
    build({ uuid: UUID }, { mail: MAIL });
    component.creatorName.set('Hans Muster');
    component.creatorAddress.set('Musterstrasse 1');
    component.creatorPhone.set('+49 123');
    component.termsAccepted.set(true);

    const first = component.submit();
    const second = component.submit();
    await Promise.all([first, second]);

    expect(backend.callCount).toBe(1);
  });

  it('reports a rejected mail address without marking the form as done', async () => {
    build({ uuid: UUID }, { mail: MAIL });
    component.creatorName.set('Hans Muster');
    component.creatorAddress.set('Musterstrasse 1');
    component.creatorPhone.set('+49 123');
    component.termsAccepted.set(true);
    backend.result = of({ message: 'EMail x@y.z nicht erfolgreich verifiziert.', success: false });

    await component.submit();

    expect(component.resultSuccess()).toBeFalse();
    expect(component.resultMessage()).toContain('nicht erfolgreich verifiziert');
    expect(component.submitting()).toBeFalse();
  });

  it('reports a failed request', async () => {
    build({ uuid: UUID }, { mail: MAIL });
    component.creatorName.set('Hans Muster');
    component.creatorAddress.set('Musterstrasse 1');
    component.creatorPhone.set('+49 123');
    component.termsAccepted.set(true);
    backend.result = throwError(() => new Error('boom'));

    await component.submit();

    expect(component.resultSuccess()).toBeFalse();
    expect(component.resultMessage()).toContain('fehlgeschlagen');
    expect(component.submitting()).toBeFalse();
  });
});
