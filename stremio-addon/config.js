/**
 * Configuração central do addon.
 *
 * Preencha com os dados do seu painel MegaEmbed (Xtream Codes).
 * Cada valor também pode vir de uma variável de ambiente, o que evita
 * deixar a senha gravada no arquivo quando o addon roda em um servidor.
 */
module.exports = {
  // Domínio do painel, com https:// e sem barra no final.
  BASE_URL: process.env.MEGAEMBED_BASE_URL || 'https://mgeb.site',

  // Credenciais da sua linha Xtream.
  USERNAME: process.env.MEGAEMBED_USERNAME || 'SEU_USUARIO',
  PASSWORD: process.env.MEGAEMBED_PASSWORD || 'SUA_SENHA',

  // Porta HTTP em que o addon escuta.
  PORT: Number(process.env.PORT) || 7000,

  // Validade do cache em segundos (2 horas, exigido pela MegaEmbed).
  CACHE_TTL_SECONDS: 7200,
};
