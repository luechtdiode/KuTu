import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { backendUrl } from '../utils';
import { ProgrammRaw, WettkampfPublic, AdminCreateCompetitionRequest, AdminCreateCompetitionResponse, AdminUpdateCompetitionRequest, AdminGetCompetitionResponse, RiegeItem, RiegeSuggestionRequest, RiegePreviewResponse, UpdateRiegeRequest, DurchgangDurationItem, Geraet, ClubRegistration, Verein, SyncAction, SyncActionKey, SyncApplyResponse, AthletRegistration, MergeDurchgangRequest, GroupDurchgangRequest, UngroupDurchgangRequest } from '../backend-types';
import {map} from "rxjs/operators";

@Injectable()
export class AdminBackendService {
  private api = backendUrl + 'api/';

  constructor(private http: HttpClient) {}

  getProgramme(): Observable<ProgrammRaw[]> {
    return this.http.get<ProgrammRaw[]>(this.api + 'competition/programmlist');
  }

  listWettkaempfe(): Observable<WettkampfPublic[]> {
    return this.http.get<WettkampfPublic[]>(this.api + 'competition');
  }

  createCompetition(request: AdminCreateCompetitionRequest): Observable<AdminCreateCompetitionResponse> {
    return this.http.post<AdminCreateCompetitionResponse>(this.api + 'admin/competition', request);
  }

  downloadCompetitionZip(uuid: string, secret: string): Observable<Blob> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get(this.api + 'competition/' + uuid, {
      headers,
      responseType: 'blob'
    });
  }

  deleteCompetition(uuid: string, secret: string): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.delete(this.api + 'competition/' + uuid, { headers });
  }

  getDisziplinenForProgram(programId: number): Observable<string[]> {
    return this.http.get<string[]>(this.api + 'competition/programmdisziplinen/' + programId);
  }

  getKategorienForProgram(programId: number): Observable<string[]> {
    return this.http.get<string[]>(this.api + 'competition/programmkategorien/' + programId);
  }

  getRiegen(uuid: string, secret: string): Observable<RiegeItem[]> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get<RiegeItem[]>(this.api + 'competition/' + uuid + '/riege', { headers });
  }

  updateRiege(uuid: string, request: UpdateRiegeRequest, secret: string): Observable<number> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put<number>(this.api + 'competition/' + uuid + '/riege', request, { headers });
  }

  suggestRiegen(uuid: string, request: RiegeSuggestionRequest, secret: string): Observable<RiegePreviewResponse> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.post<RiegePreviewResponse>(this.api + 'competition/' + uuid + '/riege/generate', request, { headers });
  }

  resetRiegen(uuid: string, secret: string): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.post(this.api + 'competition/' + uuid + '/riege/reset', null, { headers }).pipe(map(() => "OK"));
  }

  getRiegeDuration(uuid: string, secret: string): Observable<DurchgangDurationItem[]> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get<DurchgangDurationItem[]>(this.api + 'competition/' + uuid + '/riege/duration', { headers });
  }

  getDisziplinen(uuid: string): Observable<Geraet[]> {
    return this.http.get<Geraet[]>(this.api + 'durchgang/' + uuid + '/geraete');
  }

  getRegistrations(uuid: string, secret: string): Observable<ClubRegistration[]> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get<ClubRegistration[]>(this.api + 'registrations/' + uuid, { headers });
  }

  approveRegistration(uuid: string, regId: number, verein: Verein, secret: string): Observable<ClubRegistration> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put<ClubRegistration>(this.api + 'registrations/' + uuid + '/' + regId, verein, { headers });
  }

  deleteRegistration(uuid: string, regId: number, secret: string): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.delete(this.api + 'registrations/' + uuid + '/' + regId, { headers, responseType: 'text' });
  }

  getSyncActions(uuid: string, secret: string): Observable<SyncAction[]> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get<SyncAction[]>(this.api + 'registrations/' + uuid + '/syncactionsadmin', { headers});
  }
  applySyncActions(uuid: string, keys: SyncActionKey[], secret: string): Observable<SyncApplyResponse> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.post<SyncApplyResponse>(this.api + 'registrations/' + uuid + '/sync', { actions: keys }, { headers});
  }

  getAthletRegistrations(uuid: string, regId: number, secret: string): Observable<AthletRegistration[]> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get<AthletRegistration[]>(this.api + 'registrations/' + uuid + '/' + regId + '/athletes', { headers });
  }

  uploadLogo(uuid: string, secret: string, file: File): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    const formData = new FormData();
    formData.append('logo', file, file.name);
    return this.http.post(this.api + 'competition/' + uuid + '/logo', formData, { headers });
  }

  getCompetitionDetails(uuid: string, secret: string): Observable<AdminGetCompetitionResponse> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get<AdminGetCompetitionResponse>(this.api + 'admin/competition/' + uuid, { headers });
  }

  getCompetitionLogo(uuid: string, secret: string): Observable<Blob> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.get(this.api + 'competition/' + uuid + '/logo', { headers, responseType: 'blob' });
  }

  updateCompetition(uuid: string, secret: string, request: AdminUpdateCompetitionRequest): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put(this.api + 'admin/competition/' + uuid, request, { headers });
  }

  renameDurchgang(uuid: string, secret: string, oldTitle: string, newTitle: string): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put(this.api + 'competition/' + uuid + '/riege/renamedg', { oldTitle, newTitle }, { headers });
  }

  moveDurchgangToGroup(uuid: string, secret: string, request: GroupDurchgangRequest): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put(this.api + 'competition/' + uuid + '/riege/movegroup', request, { headers });
  }

  mergeDurchgang(uuid: string, secret: string, request: MergeDurchgangRequest): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put(this.api + 'competition/' + uuid + '/riege/mergedg', request, { headers });
  }

  ungroupDurchgang(uuid: string, secret: string, request: UngroupDurchgangRequest): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put(this.api + 'competition/' + uuid + '/riege/ungroup', request, { headers });
  }

  aggregateDurchgaenge(uuid: string, secret: string, request: GroupDurchgangRequest): Observable<any> {
    const headers = new HttpHeaders({ 'x-access-token': secret });
    return this.http.put(this.api + 'competition/' + uuid + '/riege/aggregate', request, { headers });
  }

}
