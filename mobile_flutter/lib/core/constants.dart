class AppConstants {
  AppConstants._();

  static const String renderApiUrl =
      'https://puntored-prueba-maai-gids6102.onrender.com';
  static const String emulatorApiUrl = 'http://10.0.2.2:8081';
  static const String localApiUrl = 'http://localhost:8081';

  static const int rolAdmin = 1;
  static const int rolUsuario = 2;

  static const List<String> estadosCiviles = [
    'Soltero',
    'Casado',
    'Divorciado',
    'Viudo',
    'Union Libre',
  ];

  static const List<String> paises = [
    'México',
    'Colombia',
    'Estados Unidos',
    'Canadá',
    'Argentina',
    'Chile',
    'Ecuador',
    'Perú',
    'España',
  ];

  static const List<String> nacionalidades = [
    'Mexicano',
    'Colombiano',
    'Estadounidense',
    'Canadiense',
    'Argentino',
    'Chileno',
    'Ecuatoriano',
    'Peruano',
  ];
}
