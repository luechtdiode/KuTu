export interface ProgrammRaw {
  id: number;
  name: string;
  aggregate: number;
  parentId: number;
  ord: number;
  alterVon: number;
  alterBis: number;
  uuid: string;
  riegenmode: number;
  bestOfCount: number;
}

export interface WettkampfPublic {
  id: number;
  uuid: string;
  datum: string;
  titel: string;
  programmId: number;
  auszeichnung: number;
  auszeichnungendnote: number;
  notificationEMail: string;
  altersklassen: string;
  jahrgangsklassen: string;
  punktegleichstandsregel: string;
  rotation: string;
  teamrule: string;
}

export interface AdminCreateCompetitionRequest {
  datum: string;
  titel: string;
  programmId: number;
  notificationEMail: string;
  auszeichnung: number;
  auszeichnungendnote: number;
  altersklassen: string;
  jahrgangsklassen: string;
  punktegleichstandsregel: string;
  rotation: string;
  teamrule: string;
  creatorName: string;
  creatorAddress: string;
  creatorPhone: string;
  termsAccepted: boolean;
  termsVersion: string;
}

export interface AdminCreateCompetitionResponse {
  uuid: string;
  titel: string;
  datum: string;
  secret: string;
}

export interface StoredSecret {
  uuid: string;
  titel: string;
  datum: string;
  secret: string;
}
