"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[6908],{6908:(q,u,g)=>{g.r(u),g.d(u,{RegAthletEditorPageModule:()=>J});var c=g(6814),d=g(95),m=g(335),a=g(3582),_=g(76),e=g(2029),p=g(4414),b=g(9253);function A(n,l){if(1&n&&(e.TgZ(0,"ion-select-option",15)(1,"ion-avatar",2),e._UZ(2,"img",8),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e._UZ(5,"br"),e.TgZ(6,"small"),e._uU(7),e.ALo(8,"date"),e.qZA()()()),2&n){const t=l.$implicit;e.Q6J("value",t.athletId),e.xp6(4),e.lnq("",t.name,", ",t.vorname," (",t.geschlecht,")"),e.xp6(3),e.hij("(",e.lcZ(8,5,t.gebdat),")")}}function f(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",19),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw(2);return e.KtG(o.selectedClubAthletId=r)}),e.YNc(1,A,9,7,"ion-select-option",20),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.selectedClubAthletId),e.xp6(1),e.Q6J("ngForOf",t.clubAthletList)}}function v(n,l){1&n&&(e.TgZ(0,"ion-item-divider")(1,"ion-avatar",0),e._UZ(2,"img",21),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4,"Einteilung"),e.qZA()())}function I(n,l){if(1&n&&(e.TgZ(0,"ion-select-option",15),e._uU(1),e.qZA()),2&n){const t=l.$implicit;e.Q6J("value",t.id),e.xp6(1),e.Oqu(t.name)}}function x(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item")(1,"ion-avatar",0),e._uU(2," \xa0 "),e.qZA(),e.TgZ(3,"ion-label",12),e._uU(4,"Programm/Kategorie"),e.qZA(),e.TgZ(5,"ion-select",22),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw(2);return e.KtG(o.registration.programId=r)}),e.YNc(6,I,2,2,"ion-select-option",20),e.qZA()()}if(2&n){const t=e.oxw(2);e.xp6(5),e.Q6J("ngModel",t.registration.programId),e.xp6(1),e.Q6J("ngForOf",t.filterPGMsForAthlet(t.registration))}}function Z(n,l){1&n&&(e.TgZ(0,"ion-item-divider")(1,"ion-avatar",0),e._UZ(2,"img",23),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4,"Team-/Mannschaftszuweisung"),e.qZA()())}function T(n,l){if(1&n&&(e.TgZ(0,"ion-row")(1,"ion-col"),e._uU(2),e.qZA()()),2&n){const t=l.$implicit;e.xp6(2),e.hij("f\xfcr ",t,"")}}function k(n,l){if(1&n&&(e.TgZ(0,"ion-select-option",15),e._uU(1),e.qZA()),2&n){const t=l.$implicit,i=e.oxw(3);e.Q6J("value",t.index),e.xp6(1),e.Oqu(i.mapTeam(t.index))}}function w(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item")(1,"ion-avatar",0),e._uU(2," \xa0 "),e.qZA(),e.TgZ(3,"ion-grid",24),e.YNc(4,T,3,1,"ion-row",25),e.TgZ(5,"ion-row")(6,"ion-col")(7,"ion-label",26),e._uU(8,"Team"),e._UZ(9,"br"),e.qZA()(),e.TgZ(10,"ion-select",27),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw(2);return e.KtG(o.registration.team=r)}),e.TgZ(11,"ion-select-option",15),e._uU(12,"Keine Teamzuordnung"),e.qZA(),e.YNc(13,k,2,2,"ion-select-option",20),e.qZA()()()()}if(2&n){const t=e.oxw(2);e.xp6(4),e.Q6J("ngForOf",t.teamrules),e.xp6(6),e.Q6J("ngModel",t.registration.team),e.xp6(1),e.Q6J("value",0),e.xp6(2),e.Q6J("ngForOf",t.teams)}}function C(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-content")(1,"form",6,7),e.NdJ("ngSubmit",function(){e.CHM(t);const r=e.MAs(2),o=e.oxw();return e.KtG(o.save(r))})("keyup.enter",function(){e.CHM(t);const r=e.MAs(2),o=e.oxw();return e.KtG(o.save(r))}),e.TgZ(3,"ion-list")(4,"ion-item-divider")(5,"ion-avatar",0),e._UZ(6,"img",8),e.qZA(),e.TgZ(7,"ion-label"),e._uU(8,"Athlet / Athletin"),e.qZA(),e.YNc(9,f,2,2,"ion-select",9),e.qZA(),e.TgZ(10,"ion-item")(11,"ion-avatar",0),e._uU(12," \xa0 "),e.qZA(),e.TgZ(13,"ion-input",10),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw();return e.KtG(o.registration.name=r)}),e.qZA(),e.TgZ(14,"ion-input",11),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw();return e.KtG(o.registration.vorname=r)}),e.qZA()(),e.TgZ(15,"ion-item")(16,"ion-avatar",0),e._uU(17," \xa0 "),e.qZA(),e.TgZ(18,"ion-label",12),e._uU(19,"Geburtsdatum"),e.qZA(),e.TgZ(20,"ion-input",13),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw();return e.KtG(o.registration.gebdat=r)}),e.qZA()(),e.TgZ(21,"ion-item")(22,"ion-avatar",0),e._uU(23," \xa0 "),e.qZA(),e.TgZ(24,"ion-label",12),e._uU(25,"Geschlecht"),e.qZA(),e.TgZ(26,"ion-select",14),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw();return e.KtG(o.registration.geschlecht=r)}),e.TgZ(27,"ion-select-option",15),e._uU(28,"weiblich"),e.qZA(),e.TgZ(29,"ion-select-option",15),e._uU(30,"m\xe4nnlich"),e.qZA()()(),e.YNc(31,v,5,0,"ion-item-divider",4),e.YNc(32,x,7,2,"ion-item",4),e.YNc(33,Z,5,0,"ion-item-divider",4),e.YNc(34,w,14,4,"ion-item",4),e.qZA(),e.TgZ(35,"ion-list")(36,"ion-button",16,17),e._UZ(38,"ion-icon",18),e._uU(39,"Speichern"),e.qZA()()()()}if(2&n){const t=e.MAs(2),i=e.oxw();e.xp6(9),e.Q6J("ngIf",0===i.registration.id&&i.clubAthletList.length>0),e.xp6(4),e.Q6J("disabled",!1)("ngModel",i.registration.name),e.xp6(1),e.Q6J("disabled",!1)("ngModel",i.registration.vorname),e.xp6(6),e.Q6J("disabled",!1)("ngModel",i.registration.gebdat),e.xp6(6),e.Q6J("ngModel",i.registration.geschlecht),e.xp6(1),e.Q6J("value","W"),e.xp6(2),e.Q6J("value","M"),e.xp6(2),e.Q6J("ngIf",i.needsPGMChoice()),e.xp6(1),e.Q6J("ngIf",i.needsPGMChoice()),e.xp6(1),e.Q6J("ngIf",i.teamsAllowed()),e.xp6(1),e.Q6J("ngIf",i.teamsAllowed()),e.xp6(2),e.Q6J("disabled",i.waiting||!i.isFormValid()||!t.valid||t.untouched)}}function M(n,l){if(1&n){const t=e.EpF();e.TgZ(0,"ion-button",28,29),e.NdJ("click",function(){e.CHM(t);const r=e.oxw();return e.KtG(r.delete())}),e._UZ(2,"ion-icon",30),e._uU(3,"Anmeldung l\xf6schen"),e.qZA()}if(2&n){const t=e.oxw();e.Q6J("disabled",t.waiting)}}const P=[{path:"",component:(()=>{class n{navCtrl;route;backendService;alertCtrl;zone;constructor(t,i,r,o,s){this.navCtrl=t,this.route=i,this.backendService=r,this.alertCtrl=o,this.zone=s}waiting=!1;registration;wettkampf;wettkampfFull;regId;athletId;wkId;wkPgms;teams;wettkampfId;clubAthletList;clubAthletListCurrent;_selectedClubAthletId;ngOnInit(){this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.athletId=parseInt(this.route.snapshot.paramMap.get("athletId")),this.backendService.getCompetitions().subscribe(t=>{const i=t.find(r=>r.uuid===this.wkId);this.wettkampfId=parseInt(i.id),this.backendService.loadProgramsForCompetition(i.uuid).subscribe(r=>{this.wkPgms=r,this.backendService.loadTeamsListForClub(this.wkId,this.regId).subscribe(o=>{this.teams=o.filter(s=>s.name?.trim().length>0),this.backendService.loadAthletListForClub(this.wkId,this.regId).subscribe(s=>{this.clubAthletList=s,this.backendService.loadAthletRegistrations(this.wkId,this.regId).subscribe(h=>{this.clubAthletListCurrent=h,this.updateUI(this.athletId?h.find(R=>R.id===this.athletId):{id:0,vereinregistrationId:this.regId,name:"",vorname:"",geschlecht:"W",gebdat:void 0,programId:void 0,team:0,registrationTime:0})})})})})})}get selectedClubAthletId(){return this._selectedClubAthletId}set selectedClubAthletId(t){this._selectedClubAthletId=t,this.registration=this.clubAthletList.find(i=>i.athletId===t)}get teamrules(){return(this.wettkampfFull.teamrule||"").split(",")}needsPGMChoice(){const t=[...this.wkPgms][0];return!(1==t.aggregate&&t.riegenmode>1)}alter(t){if(0==this.wettkampfFull.altersklassen?.trim().length){const i=new Date(t.gebdat).getFullYear();return new Date(this.wettkampfFull.datum).getFullYear()-i}{let i=Math.abs(new Date(this.wettkampfFull.datum).getTime()-new Date(t.gebdat).getTime());return Math.floor(i/864e5/365.25)}}similarRegistration(t,i){return t.athletId===i.athletId||t.name===i.name&&t.vorname===i.vorname&&t.gebdat===i.gebdat&&t.geschlecht===i.geschlecht}alternatives(t){return this.clubAthletListCurrent?.filter(i=>this.similarRegistration(i,t)&&i.id!=t.id)||[]}getAthletPgm(t){return this.wkPgms.find(i=>i.id===t.programId)||Object.assign({parent:0})}filterPGMsForAthlet(t){const i=this.alter(t),r=this.alternatives(t);return this.wkPgms.filter(o=>(o.alterVon||0)<=i&&(o.alterBis||100)>=i&&0===r.filter(s=>s.programId===o.id||this.getAthletPgm(s).parentId===o.parentId).length)}teamsAllowed(){return this.wettkampfFull.teamrule?.length>0&&"Keine Teams"!==this.wettkampfFull.teamrule}mapTeam(t){return[...this.teams.filter(i=>i.index==t).map(i=>i.index>0?i.name+" "+i.index:i.name),""][0]}editable(){return this.backendService.loggedIn}updateUI(t){this.zone.run(()=>{this.waiting=!1,this.wettkampf=this.backendService.competitionName,this.wettkampfFull=this.backendService.currentCompetition(),this.registration=Object.assign({},t),this.registration.gebdat=(0,_.tC)(this.registration.gebdat),this.registration.team||(this.registration.team=0)})}isFormValid(){return!this.registration?.programId&&!this.needsPGMChoice()&&(this.registration.programId=this.filterPGMsForAthlet(this.registration)[0]?.id),!!this.registration.gebdat&&!!this.registration.geschlecht&&this.registration.geschlecht.length>0&&!!this.registration.name&&this.registration.name.length>0&&!!this.registration.vorname&&this.registration.vorname.length>0&&!!this.registration.programId&&this.registration.programId>0&&(!this.teamsAllowed()||(!!this.registration.team||0===this.registration.team)&&!isNaN(this.registration.team))}checkPersonOverride(t){if(t.athletId){const i=[...this.clubAthletListCurrent,...this.clubAthletList].find(r=>r.athletId===t.athletId);if(i.geschlecht!==t.geschlecht||new Date((0,_.tC)(i.gebdat)).toJSON()!==new Date(t.gebdat).toJSON()||i.name!==t.name||i.vorname!==t.vorname)return!0}return!1}save(t){if(!t.valid)return;const i=Object.assign({},this.registration,{gebdat:new Date(t.value.gebdat).toJSON(),team:t.value.team?t.value.team:0});0===this.athletId||0===i.id?(this.needsPGMChoice()||this.filterPGMsForAthlet(this.registration).filter(r=>r.id!==i.programId).forEach(r=>{this.backendService.createAthletRegistration(this.wkId,this.regId,Object.assign({},i,{programId:r.id}))}),this.backendService.createAthletRegistration(this.wkId,this.regId,i).subscribe(()=>{this.navCtrl.pop()})):this.checkPersonOverride(i)?this.alertCtrl.create({header:"Achtung",subHeader:"Person \xfcberschreiben vs korrigieren",message:"Es wurden \xc4nderungen an den Personen-Feldern vorgenommen. Diese sind ausschliesslich f\xfcr Korrekturen zul\xe4ssig. Die Identit\xe4t der Person darf dadurch nicht ge\xe4ndert werden!",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"Korektur durchf\xfchren",handler:()=>{this.backendService.saveAthletRegistration(this.wkId,this.regId,i).subscribe(()=>{this.clubAthletListCurrent.filter(s=>this.similarRegistration(this.registration,s)).filter(s=>s.id!==this.registration.id).forEach(s=>{const h=Object.assign({},i,{id:s.id,registrationTime:s.registrationTime,programId:s.programId});this.backendService.saveAthletRegistration(this.wkId,this.regId,h)}),this.navCtrl.pop()})}}]}).then(s=>s.present()):this.backendService.saveAthletRegistration(this.wkId,this.regId,i).subscribe(()=>{this.navCtrl.pop()})}delete(){this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+this.registration.name+", "+this.registration.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.needsPGMChoice()||this.clubAthletListCurrent.filter(i=>this.similarRegistration(this.registration,i)).filter(i=>i.id!==this.registration.id).forEach(i=>{this.backendService.deleteAthletRegistration(this.wkId,this.regId,i)}),this.backendService.deleteAthletRegistration(this.wkId,this.regId,this.registration).subscribe(()=>{this.navCtrl.pop()})}}]}).then(i=>i.present())}static \u0275fac=function(i){return new(i||n)(e.Y36(p.SH),e.Y36(m.gz),e.Y36(b.v),e.Y36(p.Br),e.Y36(e.R0b))};static \u0275cmp=e.Xpm({type:n,selectors:[["app-reg-athlet-editor"]],decls:14,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],[3,"ngSubmit","keyup.enter"],["athletRegistrationForm","ngForm"],["src","assets/imgs/athlete.png"],["placeholder","Auswahl aus Liste","name","clubAthletid","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["required","","placeholder","Name","type","text","name","name","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Vorname","type","text","name","vorname","required","",3,"disabled","ngModel","ngModelChange"],["color","primary"],["required","","placeholder","Geburtsdatum","type","date","display-timezone","utc","name","gebdat","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Geschlecht","name","geschlecht","okText","Okay","cancelText","Abbrechen","required","",3,"ngModel","ngModelChange"],[3,"value"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name",""],["placeholder","Auswahl aus Liste","name","clubAthletid","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],["src","assets/imgs/wettkampf.png"],["required","","placeholder","Programm/Kategorie","name","programId","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],["src","assets/imgs/verein.png"],["no-padding",""],[4,"ngFor","ngForOf"],["color","primary",1,"teamlabel"],["placeholder","Team","name","team","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],["size","large","expand","block","color","danger",3,"disabled","click"],["btnDelete",""],["slot","start","name","trash"]],template:function(i,r){1&i&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title"),e._uU(5,"Anmeldung Athlet/Athletin"),e.qZA(),e.TgZ(6,"ion-note",2)(7,"div",3),e._uU(8),e.qZA()()()(),e.YNc(9,C,40,15,"ion-content",4),e.TgZ(10,"ion-footer")(11,"ion-toolbar")(12,"ion-list"),e.YNc(13,M,4,1,"ion-button",5),e.qZA()()()),2&i&&(e.xp6(8),e.hij("f\xfcr ",r.wettkampf,""),e.xp6(1),e.Q6J("ngIf",r.registration),e.xp6(4),e.Q6J("ngIf",r.athletId>0))},dependencies:[c.sg,c.O5,d._Y,d.JJ,d.JL,d.Q7,d.On,d.F,a.BJ,a.YG,a.Sm,a.wI,a.W2,a.fr,a.jY,a.Gu,a.gu,a.pK,a.Ie,a.rH,a.Q$,a.q_,a.uN,a.Nd,a.t9,a.n0,a.wd,a.sr,a.QI,a.j9,a.oU,c.uU],styles:[".teamlabel[_ngcontent-%COMP%]{padding-top:10p}"]})}return n})()}];let J=(()=>{class n{static \u0275fac=function(i){return new(i||n)};static \u0275mod=e.oAB({type:n});static \u0275inj=e.cJS({imports:[c.ez,d.u5,a.Pc,m.Bz.forChild(P)]})}return n})()}}]);