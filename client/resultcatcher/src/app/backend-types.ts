export interface WertungContainer {
  id: number;
  vorname: string;
  name: string;
  geschlecht: string;
  verein: string;
  wertung: Wertung;
}

export interface Wertung {
  id: number;
  wettkampfId: number;
  wettkampfdisziplinId: number;
  athletId: number;
  riege: string;
  endnote: number;
  noteE: number;
  noteD:number;
}

export interface Geraet {
  id: number;
  name: string;
}

export interface Wettkampf {
  id: number;
  programmId: number;
  datum: Date;
  titel: string;
  auszeichnung: number;
  auszeichnungendnote: number;
}