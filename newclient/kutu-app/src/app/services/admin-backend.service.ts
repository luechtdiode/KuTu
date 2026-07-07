import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { backendUrl } from '../utils';
import { ProgrammRaw, WettkampfPublic, AdminCreateCompetitionRequest, AdminCreateCompetitionResponse, RiegeItem, RiegeSuggestionRequest, RiegePreviewResponse, UpdateRiegeRequest, DurchgangDurationItem, Geraet } from '../backend-types';
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

  getRiegen(uuid: string): Observable<RiegeItem[]> {
    return this.http.get<RiegeItem[]>(this.api + 'competition/' + uuid + '/riege');
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
}
