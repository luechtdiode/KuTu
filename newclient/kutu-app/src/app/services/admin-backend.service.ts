import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { backendUrl } from '../utils';
import { ProgrammRaw, WettkampfPublic, AdminCreateCompetitionRequest, AdminCreateCompetitionResponse } from '../backend-types';

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
}
